package com.subastas.consignacion;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.response.ConsignacionResponse;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsignacionControllerTest extends BaseIntegrationTest {

    private String jwtJuan;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
    }

    @Test
    @Order(1)
    void listar_consignaciones_propias() {
        ResponseEntity<ConsignacionResponse[]> res = getWithAuth(
                "/api/v1/consignaciones", jwtJuan, ConsignacionResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    @Order(2)
    void crear_consignacion_con_menos_de_6_fotos_falla() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtJuan);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("descripcion", "Objeto de prueba");
        body.add("acepta_pertenencia", "true");
        body.add("cuenta_destino_id", "1");
        // Solo 2 fotos (requiere 6)
        for (int i = 0; i < 2; i++) {
            body.add("fotos", fakeImage("foto" + i + ".jpg"));
        }

        ResponseEntity<Map<String, Object>> res = rest.exchange(
                "/api/v1/consignaciones", HttpMethod.POST,
                new HttpEntity<>(body, headers), MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(3)
    void crear_consignacion_sin_declarar_pertenencia_falla() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtJuan);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("descripcion", "Objeto de prueba");
        body.add("acepta_pertenencia", "false");
        body.add("cuenta_destino_id", "1");
        for (int i = 0; i < 6; i++) {
            body.add("fotos", fakeImage("foto" + i + ".jpg"));
        }

        ResponseEntity<Map<String, Object>> res = rest.exchange(
                "/api/v1/consignaciones", HttpMethod.POST,
                new HttpEntity<>(body, headers), MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void aceptar_condiciones_consignacion_aceptada() {
        // La consignación 1 (del DataLoader) está en estado ACEPTADA
        ResponseEntity<ConsignacionResponse> res = postWithAuth(
                "/api/v1/consignaciones/1/aceptar-condiciones", jwtJuan, null, ConsignacionResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("aceptadas");
    }

    @Test
    @Order(5)
    void consignaciones_sin_jwt_es_rechazado() {
        ResponseEntity<Map<String, Object>> res = getNoAuth("/api/v1/consignaciones", MAP_TYPE);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    private ByteArrayResource fakeImage(String filename) {
        // JPEG magic bytes: FF D8 FF
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        return new ByteArrayResource(jpegBytes) {
            @Override
            public String getFilename() { return filename; }
        };
    }
}
