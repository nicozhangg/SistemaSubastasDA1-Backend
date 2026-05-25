package com.subastas.usuario;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.request.PagarMultaRequest;
import com.subastas.model.dto.response.*;
import com.subastas.model.entity.Multa;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoMulta;
import com.subastas.model.enums.Moneda;
import com.subastas.model.enums.TipoMedioPago;
import com.subastas.repository.MultaRepository;
import com.subastas.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioControllerTest extends BaseIntegrationTest {

    @Autowired
    private MultaRepository multaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private String jwtJuan;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
    }

    // ---- Perfil ----

    @Test
    @Order(1)
    void obtener_perfil_devuelve_datos_del_usuario() {
        ResponseEntity<UsuarioResponse> res = getWithAuth("/api/v1/usuarios/perfil", jwtJuan, UsuarioResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getEmail()).isEqualTo("juan@test.com");
        assertThat(res.getBody().getNombre()).isEqualTo("Juan");
    }

    @Test
    @Order(2)
    void perfil_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/usuarios/perfil", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Medios de pago ----

    @Test
    @Order(3)
    void listar_medios_pago() {
        ResponseEntity<MedioPagoResponse[]> res = getWithAuth(
                "/api/v1/usuarios/medios-pago", jwtJuan, MedioPagoResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    @Order(4)
    void agregar_medio_pago() {
        MedioPagoRequest req = new MedioPagoRequest();
        req.setTipo(TipoMedioPago.CUENTA_BANCARIA);
        req.setAlias("Cuenta Test");
        req.setMoneda(Moneda.ARS);
        req.setBanco("Banco Test");
        req.setNumeroCuenta("111222333");
        req.setTipoCuenta("nacional");
        req.setCbu("0000000000000000000001");

        ResponseEntity<MedioPagoResponse> res = postWithAuth(
                "/api/v1/usuarios/medios-pago", jwtJuan, req, MedioPagoResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().isVerificado()).isTrue();
        assertThat(res.getBody().getAlias()).isEqualTo("Cuenta Test");
    }

    @Test
    @Order(5)
    void eliminar_medio_pago_mientras_conectado_falla() {
        // Conectar a subasta
        ConectarSubastaRequest conectar = new ConectarSubastaRequest();
        conectar.setMedioPagoId(1L);
        postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, conectar, Map.class);

        // Intentar eliminar medio de pago
        ResponseEntity<Map> res = deleteWithAuth("/api/v1/usuarios/medios-pago/1", jwtJuan, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Limpiar
        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, Map.class);
    }

    // ---- Multas ----

    @Test
    @Order(6)
    void pagar_multa_actualiza_estado_y_contador() {
        Usuario usuario = usuarioRepository.findByEmail("juan@test.com").orElseThrow();

        Multa multa = Multa.builder()
                .monto(new BigDecimal("5000.00"))
                .motivo("Multa de prueba")
                .usuario(usuario)
                .fechaLimitePago(LocalDateTime.now().plusDays(3))
                .build();
        multa = multaRepository.save(multa);

        usuario.setMultasPendientes(1);
        usuarioRepository.save(usuario);

        PagarMultaRequest req = new PagarMultaRequest();
        req.setMedioPagoId(1L);

        ResponseEntity<MultaResponse> res = postWithAuth(
                "/api/v1/usuarios/multas/" + multa.getId() + "/pagar", jwtJuan, req, MultaResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getEstado()).isEqualTo(EstadoMulta.PAGADA);
        assertThat(res.getBody().getPuedeParticiparNuevamente()).isTrue();

        Usuario actualizado = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        assertThat(actualizado.getMultasPendientes()).isZero();
    }

    // ---- Métricas ----

    @Test
    @Order(7)
    void obtener_metricas() {
        ResponseEntity<MetricasResponse> res = getWithAuth(
                "/api/v1/usuarios/metricas", jwtJuan, MetricasResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMultasPendientes()).isGreaterThanOrEqualTo(0);
    }

    // ---- Participaciones ----

    @Test
    @Order(8)
    void listar_participaciones() {
        ResponseEntity<ParticipacionResponse[]> res = getWithAuth(
                "/api/v1/usuarios/participaciones", jwtJuan, ParticipacionResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
