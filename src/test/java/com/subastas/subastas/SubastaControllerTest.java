package com.subastas.subastas;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.response.ConectarSubastaResponse;
import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubastaControllerTest extends BaseIntegrationTest {

    private String jwtJuan;
    private String jwtMaria;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
        jwtMaria = loginAndGetToken("maria@test.com", "password123");
    }

    // ---- Listar ----

    @Test
    @Order(1)
    void listar_subastas_requiere_autenticacion() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/subastas", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(2)
    void listar_subastas_autenticado_devuelve_resultados() {
        ResponseEntity<Map> res = getWithAuth("/api/v1/subastas", jwtJuan, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsKey("data");
    }

    // ---- Catálogo público ----

    @Test
    @Order(3)
    void catalogo_es_publico_sin_jwt() {
        ResponseEntity<Object[]> res = rest.getForEntity("/api/v1/subastas/1/catalogo", Object[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    // ---- Conectar / Desconectar ----

    @Test
    @Order(4)
    void conectar_a_subasta_abierta_exitoso() {
        ConectarSubastaRequest req = new ConectarSubastaRequest();
        req.setMedioPagoId(1L);

        ResponseEntity<ConectarSubastaResponse> res = postWithAuth(
                "/api/v1/subastas/1/conectar", jwtJuan, req, ConectarSubastaResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getSesionId()).isNotNull();

        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, Map.class);
    }

    @Test
    @Order(5)
    void conectar_a_dos_subastas_simultaneamente_falla() {
        ConectarSubastaRequest req = new ConectarSubastaRequest();
        req.setMedioPagoId(1L);
        postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, req, ConectarSubastaResponse.class);

        ResponseEntity<Map> res = postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, req, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, Map.class);
    }

    @Test
    @Order(6)
    void desconectar_sin_estar_conectado_falla() {
        ResponseEntity<Map> res = postWithAuth("/api/v1/subastas/1/desconectar", jwtMaria, null, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(7)
    void estado_puja_sin_estar_conectado_falla() {
        ResponseEntity<Map> res = getWithAuth("/api/v1/subastas/1/pujas/estado", jwtMaria, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(8)
    void obtener_subasta_inexistente_devuelve_404() {
        ResponseEntity<Map> res = getWithAuth("/api/v1/subastas/9999", jwtJuan, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
