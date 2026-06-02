package com.subastas.auth;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.RegistroPaso2Request;
import com.subastas.model.dto.response.LoginResponse;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoUsuario;
import com.subastas.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ---- Login ----

    @Test
    void login_exitoso_devuelve_jwt_y_datos_usuario() {
        ResponseEntity<LoginResponse> res = postNoAuth(
                "/api/v1/auth/login",
                Map.of("email", "juan@test.com", "password", "password123"),
                LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = res.getBody();
        assertThat(body.getTokenAcceso()).isNotBlank();
        assertThat(body.getUsuario().getEmail()).isEqualTo("juan@test.com");
    }

    @Test
    void login_con_credenciales_incorrectas_devuelve_401() {
        // Usar rawRest para evitar que HttpURLConnection reintente en 401
        ResponseEntity<Map<String, Object>> res = postNoAuthRaw(
                "/api/v1/auth/login",
                Map.of("email", "juan@test.com", "password", "wrongpassword"),
                MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_con_email_inexistente_devuelve_401() {
        ResponseEntity<Map<String, Object>> res = postNoAuthRaw(
                "/api/v1/auth/login",
                Map.of("email", "noexiste@test.com", "password", "password123"),
                MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_usuario_bloqueado_no_permite_acceso() {
        Usuario usuario = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        EstadoUsuario estadoOriginal = usuario.getEstado();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuarioRepository.save(usuario);

        try {
            ResponseEntity<Map<String, Object>> res = postNoAuthRaw(
                    "/api/v1/auth/login",
                    Map.of("email", "juan@test.com", "password", "password123"),
                    MAP_TYPE);

            assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        } finally {
            usuario.setEstado(estadoOriginal);
            usuarioRepository.save(usuario);
        }
    }

    // ---- Registro paso 1: email duplicado ----

    @Test
    void registro_paso1_email_duplicado_devuelve_409() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        body.add("nombre", "Otro");
        body.add("apellido", "Registro");
        body.add("email", "juan@test.com");
        body.add("numeroDni", "11111111");
        body.add("domicilioLegal", "Calle X");
        body.add("paisOrigen", "Argentina");
        body.add("foto_dni_frente", new org.springframework.core.io.ByteArrayResource(new byte[]{1}) {
            @Override public String getFilename() { return "f.jpg"; }
        });
        body.add("foto_dni_dorso", new org.springframework.core.io.ByteArrayResource(new byte[]{1}) {
            @Override public String getFilename() { return "d.jpg"; }
        });

        ResponseEntity<Map<String, Object>> res = rest.exchange(
                "/api/v1/auth/registro/paso1", HttpMethod.POST,
                new HttpEntity<>(body, headers), MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---- Registro paso 2 ----

    @Test
    void registro_paso2_con_token_invalido_devuelve_400() {
        RegistroPaso2Request req = new RegistroPaso2Request();
        req.setEmail("juan@test.com");
        req.setTokenEmail("token-falso-inexistente");
        req.setPassword("miPassword123");

        ResponseEntity<Map<String, Object>> res = postNoAuth("/api/v1/auth/registro/paso2", req, MAP_TYPE);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
