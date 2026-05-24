package com.subastas.controller;

import com.subastas.model.dto.response.ConsignacionResponse;
import com.subastas.model.dto.response.PolizaResponse;
import com.subastas.model.dto.response.UbicacionResponse;
import com.subastas.model.entity.Usuario;
import com.subastas.service.ConsignacionService;
import com.subastas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints de consignación: permite a un usuario solicitar que un bien propio
 * sea incluido en subasta, aceptar o rechazar las condiciones propuestas por la
 * empresa, y consultar la ubicación en depósito y la póliza de seguro.
 */
@Validated
@RestController
@RequestMapping("/api/v1/consignaciones")
@RequiredArgsConstructor
public class ConsignacionController {

    private final ConsignacionService consignacionService;
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<ConsignacionResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(consignacionService.listar(usuario));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConsignacionResponse> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @NotBlank(message = "La descripción es obligatoria") @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "datos_adicionales", required = false) String datosAdicionales,
            @RequestParam("acepta_pertenencia") boolean aceptaPertenencia,
            @RequestParam("cuenta_destino_id") Long cuentaDestinoId,
            @RequestParam(value = "precio_sugerido", required = false) java.math.BigDecimal precioSugerido,
            @RequestPart("fotos") List<MultipartFile> fotos) {

        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        ConsignacionResponse response = consignacionService.crear(
                usuario, descripcion, datosAdicionales, aceptaPertenencia, cuentaDestinoId, precioSugerido, fotos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/aceptar-condiciones")
    public ResponseEntity<ConsignacionResponse> aceptarCondiciones(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(consignacionService.aceptarCondiciones(id, usuario));
    }

    @PostMapping("/{id}/rechazar-condiciones")
    public ResponseEntity<ConsignacionResponse> rechazarCondiciones(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(consignacionService.rechazarCondiciones(id, usuario));
    }

    @GetMapping("/{id}/ubicacion")
    public ResponseEntity<UbicacionResponse> obtenerUbicacion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(consignacionService.obtenerUbicacion(id, usuario));
    }

    @GetMapping("/{id}/poliza")
    public ResponseEntity<PolizaResponse> obtenerPoliza(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
        return ResponseEntity.ok(consignacionService.obtenerPoliza(id, usuario));
    }
}
