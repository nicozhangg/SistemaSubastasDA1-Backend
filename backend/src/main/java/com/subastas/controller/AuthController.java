package com.subastas.controller;

import com.subastas.model.dto.request.LoginRequest;
import com.subastas.model.dto.request.RegistroPaso1Request;
import com.subastas.model.dto.request.RegistroPaso2Request;
import com.subastas.model.dto.response.LoginResponse;
import com.subastas.model.dto.response.RegistroResponse;
import com.subastas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoints de autenticación: registro en dos pasos, login y logout.
 * El registro requiere primero datos personales + fotos DNI (paso 1),
 * y luego la activación por token de email (paso 2).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Paso 1: datos personales y fotos del DNI. Deja la cuenta en PENDIENTE_VERIFICACION. */
    @PostMapping(value = "/registro/paso1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RegistroResponse> registroPaso1(
            @Valid @ModelAttribute RegistroPaso1Request request,
            @RequestPart(value = "foto_dni_frente", required = false) MultipartFile fotoDniFrente,
            @RequestPart(value = "foto_dni_dorso", required = false) MultipartFile fotoDniDorso) {

        RegistroResponse response = authService.registroPaso1(request, fotoDniFrente, fotoDniDorso);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Paso 2: validación del token de email + seteo de contraseña. Activa la cuenta y devuelve JWT. */
    @PostMapping("/registro/paso2")
    public ResponseEntity<RegistroResponse> registroPaso2(@Valid @RequestBody RegistroPaso2Request request) {
        return ResponseEntity.ok(authService.registroPaso2(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // Con JWT stateless, el logout es responsabilidad del cliente (eliminar el token)
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada exitosamente"));
    }
}
