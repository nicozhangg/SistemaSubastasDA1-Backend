package com.subastas.websocket;

import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.request.LoginRequest;
import com.subastas.model.dto.response.LoginResponse;
import com.subastas.model.dto.websocket.PujaWebSocketRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración WebSocket para el flujo de pujas.
 *
 * Escenarios cubiertos:
 *  1. Puja válida → postor recibe BID_CONFIRMED, rival recibe BID_UPDATED
 *  2. Puja inválida (monto fuera de rango) → postor recibe BID_REJECTED
 *  3. Puja sin autenticación → servidor la ignora silenciosamente
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PujaWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String jwtJuan;
    private String jwtMaria;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        wsUrl = "http://localhost:" + port + "/ws";
        jwtJuan  = login("juan@test.com",  "password123");
        jwtMaria = login("maria@test.com", "password123");
    }

    // -------------------------------------------------------------------------
    // Test 1: flujo completo — confirmación + broadcast + rechazo
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void flujo_completo_puja_confirmada_broadcast_y_rechazo() throws Exception {
        conectarASubasta(jwtJuan,  1L, 1L);
        conectarASubasta(jwtMaria, 1L, 2L);

        // Colas separadas por canal para evitar mezclar mensajes de topic y cola privada
        BlockingQueue<Map> juanPrivate  = new LinkedBlockingQueue<>();
        BlockingQueue<Map> juanTopic    = new LinkedBlockingQueue<>();
        BlockingQueue<Map> mariaPrivate = new LinkedBlockingQueue<>();
        BlockingQueue<Map> mariaTopic   = new LinkedBlockingQueue<>();

        WebSocketStompClient clientJuan  = crearStompClient();
        WebSocketStompClient clientMaria = crearStompClient();

        StompSession sessionJuan = clientJuan
                .connectAsync(wsUrl, (WebSocketHttpHeaders) null, crearHeadersConJwt(jwtJuan), new LoggingSessionHandler("Juan"))
                .get(5, TimeUnit.SECONDS);

        StompSession sessionMaria = clientMaria
                .connectAsync(wsUrl, (WebSocketHttpHeaders) null, crearHeadersConJwt(jwtMaria), new LoggingSessionHandler("María"))
                .get(5, TimeUnit.SECONDS);

        sessionJuan .subscribe("/user/queue/pujas", buildHandler(juanPrivate));
        sessionMaria.subscribe("/user/queue/pujas", buildHandler(mariaPrivate));
        sessionJuan .subscribe("/topic/subastas/1", buildHandler(juanTopic));
        sessionMaria.subscribe("/topic/subastas/1", buildHandler(mariaTopic));

        Thread.sleep(300); // espera que las suscripciones se registren en el broker

        // ---- Juan puja 55000 (válido: base=50000, mín=50500, máx=60000) ----
        enviarPuja(sessionJuan, 1L, 1L, "55000.00", 1L);

        Map confirmedJuan = juanPrivate.poll(5, TimeUnit.SECONDS);
        assertThat(confirmedJuan)
                .as("Juan debe recibir BID_CONFIRMED")
                .isNotNull()
                .containsEntry("tipo", "BID_CONFIRMED");
        assertThat(confirmedJuan.get("monto").toString())
                .as("Monto confirmado debe ser 55000")
                .contains("55000");

        Map updatedMaria = mariaTopic.poll(5, TimeUnit.SECONDS);
        assertThat(updatedMaria)
                .as("María debe recibir BID_UPDATED por el topic")
                .isNotNull()
                .containsEntry("tipo", "BID_UPDATED");
        assertThat(updatedMaria.get("nuevaMejorOferta").toString())
                .as("Mejor oferta difundida debe ser 55000")
                .contains("55000");

        // Juan también recibe el BID_UPDATED de su propia puja vía topic — consumirlo antes de la siguiente
        juanTopic.poll(2, TimeUnit.SECONDS);

        // ---- María puja 60000 (válido: después de 55000, mín=55500, máx=65000) ----
        enviarPuja(sessionMaria, 1L, 1L, "60000.00", 2L);

        Map confirmedMaria = mariaPrivate.poll(5, TimeUnit.SECONDS);
        assertThat(confirmedMaria)
                .as("María debe recibir BID_CONFIRMED")
                .isNotNull()
                .containsEntry("tipo", "BID_CONFIRMED");

        Map updatedJuan = juanTopic.poll(5, TimeUnit.SECONDS);
        assertThat(updatedJuan)
                .as("Juan debe recibir BID_UPDATED con la oferta de María")
                .isNotNull()
                .containsEntry("tipo", "BID_UPDATED");
        assertThat(updatedJuan.get("nuevaMejorOferta").toString())
                .contains("60000");

        // ---- Juan puja 50000 (inválido: por debajo del mínimo 60500) ----
        enviarPuja(sessionJuan, 1L, 1L, "50000.00", 1L);

        Map rechazado = juanPrivate.poll(5, TimeUnit.SECONDS);
        assertThat(rechazado)
                .as("Juan debe recibir BID_REJECTED por monto inválido")
                .isNotNull()
                .containsEntry("tipo", "BID_REJECTED");
        assertThat(rechazado.get("motivo")).isNotNull();

        sessionJuan.disconnect();
        sessionMaria.disconnect();

        desconectarDeSubasta(jwtJuan,  1L);
        desconectarDeSubasta(jwtMaria, 1L);
    }

    // -------------------------------------------------------------------------
    // Test 2: puja sin autenticación es ignorada
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    void puja_sin_autenticacion_es_ignorada() throws Exception {
        BlockingQueue<Map> mensajes = new LinkedBlockingQueue<>();

        // Conectar SIN JWT
        WebSocketStompClient client = crearStompClient();
        StompSession session = client
                .connectAsync(wsUrl, (WebSocketHttpHeaders) null, new StompHeaders(), new LoggingSessionHandler("anonimo"))
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/subastas/1", buildHandler(mensajes));
        Thread.sleep(200);

        enviarPuja(session, 1L, 1L, "55000.00", 1L);

        // El servidor ignora la puja (Principal nulo) — nada debe llegar
        Map msg = mensajes.poll(2, TimeUnit.SECONDS);
        assertThat(msg)
                .as("No debe llegar ningún mensaje a una sesión no autenticada")
                .isNull();

        session.disconnect();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String login(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/v1/auth/login", req, LoginResponse.class);
        assertThat(res.getStatusCode())
                .as("Login de %s debe ser 200", email)
                .isEqualTo(HttpStatus.OK);
        return res.getBody().getTokenAcceso();
    }

    private void conectarASubasta(String jwt, Long subastaId, Long medioPagoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConectarSubastaRequest req = new ConectarSubastaRequest();
        req.setMedioPagoId(medioPagoId);

        ResponseEntity<String> res = restTemplate.exchange(
                "/api/v1/subastas/" + subastaId + "/conectar",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void desconectarDeSubasta(String jwt, Long subastaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        try {
            restTemplate.exchange(
                    "/api/v1/subastas/" + subastaId + "/desconectar",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    String.class);
        } catch (Exception ignored) {
            // Si ya estaba desconectado, no es un error
        }
    }

    private void enviarPuja(StompSession session, Long subastaId,
                            Long itemId, String monto, Long medioPagoId) {
        StompHeaders h = new StompHeaders();
        h.setDestination("/app/subastas/" + subastaId + "/pujar");
        PujaWebSocketRequest req = new PujaWebSocketRequest();
        req.setItemId(itemId);
        req.setMonto(new BigDecimal(monto));
        req.setMedioPagoId(medioPagoId);
        session.send(h, req);
    }

    private StompFrameHandler buildHandler(BlockingQueue<Map> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return Map.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((Map) payload);
            }
        };
    }

    private WebSocketStompClient crearStompClient() {
        SockJsClient sockJs = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        WebSocketStompClient client = new WebSocketStompClient(sockJs);
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    private StompHeaders crearHeadersConJwt(String jwt) {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return headers;
    }

    private static class LoggingSessionHandler extends StompSessionHandlerAdapter {
        private final String nombre;
        LoggingSessionHandler(String nombre) { this.nombre = nombre; }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable e) {
            System.err.printf("[WS-%s] Error en frame %s: %s%n", nombre, command, e.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable e) {
            System.err.printf("[WS-%s] Error de transporte: %s%n", nombre, e.getMessage());
        }
    }
}
