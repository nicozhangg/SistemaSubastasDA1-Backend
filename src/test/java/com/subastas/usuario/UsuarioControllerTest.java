package com.subastas.usuario;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.request.PagarMultaRequest;
import com.subastas.model.dto.response.MedioPagoResponse;
import com.subastas.model.dto.response.MultaResponse;
import com.subastas.model.dto.response.UsuarioResponse;
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

class UsuarioControllerTest extends BaseIntegrationTest {

    @Autowired
    private MultaRepository multaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private String jwtJuan;
    private Long multaIdCreada;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
    }

    @AfterEach
    void cleanup() {
        // Eliminar multa creada en el test si aún existe
        if (multaIdCreada != null) {
            multaRepository.deleteById(multaIdCreada);
            multaIdCreada = null;
        }
        // Garantizar que Juan no quede conectado a una subasta si el test falla a mitad
        if (jwtJuan != null) {
            postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, MAP_TYPE);
        }
        // Sincronizar el contador de multas pendientes de Juan con el estado real de la BD
        usuarioRepository.findByEmail("juan@test.com").ifPresent(juan -> {
            long pendientes = multaRepository.countByUsuarioAndEstado(juan, EstadoMulta.PENDIENTE);
            juan.setMultasPendientes((int) pendientes);
            usuarioRepository.save(juan);
        });
    }

    // ---- Perfil ----

    @Test
    void obtener_perfil_devuelve_datos_del_usuario() {
        ResponseEntity<UsuarioResponse> res = getWithAuth("/api/v1/usuarios/perfil", jwtJuan, UsuarioResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getEmail()).isEqualTo("juan@test.com");
        assertThat(res.getBody().getNombre()).isEqualTo("Juan");
    }

    @Test
    void perfil_sin_jwt_es_rechazado() {
        ResponseEntity<Map<String, Object>> res =getNoAuth("/api/v1/usuarios/perfil", MAP_TYPE);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Medios de pago ----

    @Test
    void listar_medios_pago() {
        ResponseEntity<MedioPagoResponse[]> res = getWithAuth(
                "/api/v1/usuarios/medios-pago", jwtJuan, MedioPagoResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
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
    void eliminar_medio_pago_mientras_conectado_falla() {
        // Conectar a subasta
        ConectarSubastaRequest conectar = new ConectarSubastaRequest();
        conectar.setMedioPagoId(1L);
        postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, conectar, MAP_TYPE);

        // Intentar eliminar medio de pago
        ResponseEntity<Map<String, Object>> res =deleteWithAuth("/api/v1/usuarios/medios-pago/1", jwtJuan, MAP_TYPE);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Limpiar
        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, MAP_TYPE);
    }

    // ---- Multas ----

    @Test
    void pagar_multa_actualiza_estado_y_contador() {
        Usuario usuario = usuarioRepository.findByEmail("juan@test.com").orElseThrow();

        Multa multa = Multa.builder()
                .monto(new BigDecimal("5000.00"))
                .motivo("Multa de prueba")
                .usuario(usuario)
                .fechaLimitePago(LocalDateTime.now().plusDays(3))
                .build();
        multa = multaRepository.save(multa);
        multaIdCreada = multa.getId();

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

}
