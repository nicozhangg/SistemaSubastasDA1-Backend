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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerTest extends BaseIntegrationTest {

    @Autowired private CompraRepository compraRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private MedioPagoRepository medioPagoRepository;
    @Autowired private MensajeChatRepository mensajeChatRepository;

    private String jwtJuan;
    private String jwtMaria;
    private Long compraId;
    // Registra todos los IDs de compras creadas para que @AfterEach los limpie
    private final List<Long> compraIdsParaLimpiar = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
        jwtMaria = loginAndGetToken("maria@test.com", "password123");
        compraIdsParaLimpiar.clear();

        // Siempre crear una compra fresca para aislar cada test
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();
        MedioPago mp = medioPagoRepository.findByUsuario(juan).get(0);

        Compra compra = compraRepository.save(Compra.builder()
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
                .build());
        compraId = compra.getId();
        compraIdsParaLimpiar.add(compraId);
    }

    @AfterEach
    void cleanup() {
        for (Long id : compraIdsParaLimpiar) {
            compraRepository.findById(id).ifPresent(c -> {
                mensajeChatRepository.findByCompraOrderByTimestampAsc(c)
                        .forEach(mensajeChatRepository::delete);
                compraRepository.delete(c);
            });
        }
        compraIdsParaLimpiar.clear();
    }

    @Test
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
    void obtener_mensajes_del_chat() {
        // Enviar un mensaje propio para garantizar que el chat no esté vacío
        MensajeChatRequest req = new MensajeChatRequest();
        req.setContenido("Mensaje para verificar listado");
        postWithAuth("/api/v1/compras/" + compraId + "/chat", jwtJuan, req, MensajeChatResponse.class);

        ResponseEntity<MensajeChatResponse[]> res = getWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtJuan, MensajeChatResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void otro_usuario_no_puede_ver_chat_ajeno() {
        ResponseEntity<Map<String, Object>> res = getWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtMaria, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void confirmar_entrega_y_reintento_falla() {
        EntregaRequest req = new EntregaRequest();
        req.setModalidadEntrega(ModalidadEntrega.ENVIO_DOMICILIO);
        req.setDireccionEnvio("Av. Siempre Viva 742, Springfield");

        ResponseEntity<CompraResponse> res = patchWithAuth(
                "/api/v1/compras/" + compraId + "/entrega", jwtJuan, req, CompraResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getModalidadEntrega()).isEqualTo(ModalidadEntrega.ENVIO_DOMICILIO);
        assertThat(res.getBody().getDireccionEnvio()).isEqualTo("Av. Siempre Viva 742, Springfield");
        assertThat(res.getBody().isCoberturaSeguroActiva()).isTrue();

        // Segundo intento: la modalidad ya fue confirmada, debe rechazarse
        EntregaRequest req2 = new EntregaRequest();
        req2.setModalidadEntrega(ModalidadEntrega.RETIRO_PERSONAL);
        ResponseEntity<Map<String, Object>> res2 = patchWithAuth(
                "/api/v1/compras/" + compraId + "/entrega", jwtJuan, req2, MAP_TYPE);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void retiro_personal_desactiva_seguro() {
        // Usa item2 para no colisionar con la compra de @BeforeEach (item1)
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        Item item2 = itemRepository.findById(2L).orElseThrow();
        MedioPago mp = medioPagoRepository.findByUsuario(juan).get(0);

        Compra compra2 = compraRepository.save(Compra.builder()
                .item(item2).usuario(juan).montoOfertado(new BigDecimal("80000.00"))
                .comisiones(BigDecimal.ZERO).costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("80000.00")).moneda(Moneda.ARS).medioPago(mp)
                .estadoPago(EstadoPago.PENDIENTE).fechaLimitePago(LocalDateTime.now().plusHours(72))
                .build());
        compraIdsParaLimpiar.add(compra2.getId());

        EntregaRequest req = new EntregaRequest();
        req.setModalidadEntrega(ModalidadEntrega.RETIRO_PERSONAL);

        ResponseEntity<CompraResponse> res = patchWithAuth(
                "/api/v1/compras/" + compra2.getId() + "/entrega", jwtJuan, req, CompraResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getModalidadEntrega()).isEqualTo(ModalidadEntrega.RETIRO_PERSONAL);
        assertThat(res.getBody().isCoberturaSeguroActiva()).isFalse();
    }

    @Test
    void enviar_mensaje_vacio_falla() {
        MensajeChatRequest req = new MensajeChatRequest();
        req.setContenido("");

        ResponseEntity<Map<String, Object>> res = postWithAuth(
                "/api/v1/compras/" + compraId + "/chat", jwtJuan, req, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void chat_sin_jwt_es_rechazado() {
        ResponseEntity<Map<String, Object>> res = getNoAuth(
                "/api/v1/compras/" + compraId + "/chat", MAP_TYPE);

        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
