package com.subastas.seguridad;

import com.subastas.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los endpoints protegidos rechazan requests sin JWT
 * y que los endpoints públicos funcionan sin autenticación.
 * Spring Security 6 devuelve 403 por defecto cuando no hay AuthenticationEntryPoint configurado.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeguridadTest extends BaseIntegrationTest {

    // ---- Endpoints protegidos sin JWT → 403 ----

    @Test
    @Order(1)
    void subastas_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/subastas", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(2)
    void perfil_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/usuarios/perfil", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(3)
    void medios_pago_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/usuarios/medios-pago", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(4)
    void multas_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/usuarios/multas", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(5)
    void consignaciones_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = getNoAuth("/api/v1/consignaciones", Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(6)
    void conectar_subasta_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = postNoAuth(
                "/api/v1/subastas/1/conectar", Map.of("medioPagoId", 1), Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(7)
    void pujar_sin_jwt_es_rechazado() {
        ResponseEntity<Map> res = postNoAuth(
                "/api/v1/subastas/1/pujas",
                Map.of("itemId", 1, "monto", 55000, "medioPagoId", 1), Map.class);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Endpoints públicos sin JWT → 200 ----

    @Test
    @Order(8)
    void catalogo_sin_jwt_devuelve_200() {
        ResponseEntity<Object[]> res = rest.getForEntity("/api/v1/subastas/1/catalogo", Object[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(9)
    void item_detalle_sin_jwt_devuelve_200() {
        ResponseEntity<Map> res = rest.getForEntity("/api/v1/items/1", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(10)
    void imagenes_item_sin_jwt_devuelve_200() {
        ResponseEntity<Object[]> res = rest.getForEntity("/api/v1/items/1/imagenes", Object[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- JWT inválido → rechazado ----

    @Test
    @Order(11)
    void jwt_invalido_es_rechazado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token.invalido.falso");

        ResponseEntity<Map> res = rest.exchange("/api/v1/usuarios/perfil",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Acceso a recurso de otro usuario ----

    @Test
    @Order(12)
    void usuario_no_puede_ver_compra_ajena() {
        String jwtMaria = loginAndGetToken("maria@test.com", "password123");

        ResponseEntity<Map> res = getWithAuth("/api/v1/usuarios/compras/1", jwtMaria, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
