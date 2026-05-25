package com.subastas.chat;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.EntregaRequest;
import com.subastas.model.dto.request.MensajeChatRequest;
import com.subastas.model.dto.response.CompraResponse;
import com.subastas.model.dto.response.MensajeChatResponse;
import com.subastas.model.entity.*;
import com.subastas.model.enums.*;
import com.subastas.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatControllerTest extends BaseIntegrationTest {

    @Autowired private CompraRepository compraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private MedioPagoRepository medioPagoRepository;

    private String jwtJuan;
    private String jwtMaria;
    private Long compraId;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
        jwtMaria = loginAndGetToken("maria@test.com", "password123");

        // Crear una compra de prueba para Juan si no existe
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        var compras = compraRepository.findByUsuarioOrderByIdDesc(juan);
        if (compras.isEmpty()) {
            Item item = itemRepository.findAll().get(0);
            MedioPago mp = medioPagoRepository.findByUsuario(juan).get(0);
            Compra compra = Compra.builder()
                    .item(item)
                    .usuario(juan)
                    .montoOfertado(new BigDecimal("55000.00"))
                    .comisiones(BigDecimal.ZERO)
                    .costoEnvio(BigDecimal.ZERO)
                    .total(new BigDecimal("55000.00"))
                    .moneda(Moneda.ARS)
                    .medioPago(mp)
                    .estadoPago(EstadoPago.PENDIENTE)
                    .fechaLimitePago(LocalDateTime.now().plusHours(72))
                    .build();
            compra = compraRepository.save(compra);
            compraId = compra.getId();
        } else {
            compraId = compras.get(0).getId();
        }
    }

    @Test
    @Order(1)
    void enviar_mensaje_al_chat() {
        MensajeChatRequest req = new MensajeChatRequest();
        req.setContenido("Hola, quiero coordinar la entrega");

        ResponseEntity<MensajeChatResponse> res = postWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtJuan, req, MensajeChatResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().getContenido()).isEqualTo("Hola, quiero coordinar la entrega");
        assertThat(res.getBody().getRemitente()).isEqualTo(RemitenteMensaje.USUARIO);
    }

    @Test
    @Order(2)
    void obtener_mensajes_del_chat() {
        ResponseEntity<MensajeChatResponse[]> res = getWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtJuan, MensajeChatResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    @Order(3)
    void otro_usuario_no_puede_ver_chat_ajeno() {
        ResponseEntity<Map<String, Object>> res = getWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtMaria, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(4)
    void confirmar_envio_a_domicilio() {
        EntregaRequest req = new EntregaRequest();
        req.setModalidadEntrega(ModalidadEntrega.ENVIO_DOMICILIO);
        req.setDireccionEnvio("Av. Siempre Viva 742, Springfield");

        ResponseEntity<CompraResponse> res = patchWithAuth(
                "/api/v1/compras/" + compraId + "/entrega", jwtJuan, req, CompraResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getModalidadEntrega()).isEqualTo(ModalidadEntrega.ENVIO_DOMICILIO);
        assertThat(res.getBody().getDireccionEnvio()).isEqualTo("Av. Siempre Viva 742, Springfield");
        assertThat(res.getBody().isCoberturaSeguroActiva()).isTrue();
    }

    @Test
    @Order(5)
    void confirmar_entrega_dos_veces_falla() {
        EntregaRequest req = new EntregaRequest();
        req.setModalidadEntrega(ModalidadEntrega.RETIRO_PERSONAL);

        ResponseEntity<Map<String, Object>> res = patchWithAuth(
                "/api/v1/compras/" + compraId + "/entrega", jwtJuan, req, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    void retiro_personal_desactiva_seguro() {
        // Crear nueva compra para este test
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        Item item = itemRepository.findAll().get(1);
        MedioPago mp = medioPagoRepository.findByUsuario(juan).get(0);
        Compra compra2 = compraRepository.save(Compra.builder()
                .item(item).usuario(juan).montoOfertado(new BigDecimal("80000.00"))
                .comisiones(BigDecimal.ZERO).costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("80000.00")).moneda(Moneda.ARS).medioPago(mp)
                .estadoPago(EstadoPago.PENDIENTE).fechaLimitePago(LocalDateTime.now().plusHours(72))
                .build());

        EntregaRequest req = new EntregaRequest();
        req.setModalidadEntrega(ModalidadEntrega.RETIRO_PERSONAL);

        ResponseEntity<CompraResponse> res = patchWithAuth(
                "/api/v1/compras/" + compra2.getId() + "/entrega", jwtJuan, req, CompraResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getModalidadEntrega()).isEqualTo(ModalidadEntrega.RETIRO_PERSONAL);
        assertThat(res.getBody().isCoberturaSeguroActiva()).isFalse();
    }

    @Test
    @Order(7)
    void enviar_mensaje_vacio_falla() {
        MensajeChatRequest req = new MensajeChatRequest();
        req.setContenido("");

        ResponseEntity<Map<String, Object>> res = postWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtJuan, req, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(8)
    void chat_sin_jwt_es_rechazado() {
        ResponseEntity<Map<String, Object>> res = getNoAuth(
                "/api/v1/compras/" + compraId + "/chat", MAP_TYPE);

        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
