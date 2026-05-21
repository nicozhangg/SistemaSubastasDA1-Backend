package com.subastas.controller;

import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.request.PagarMultaRequest;
import com.subastas.model.dto.response.*;
import com.subastas.service.CompraService;
import com.subastas.service.MultaService;
import com.subastas.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del área personal del usuario: perfil, medios de pago,
 * multas, compras ganadas y métricas de participación.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final MultaService multaService;
    private final CompraService compraService;

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> obtenerPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(userDetails.getUsername()));
    }

    @GetMapping("/medios-pago")
    public ResponseEntity<List<MedioPagoResponse>> listarMediosPago(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.listarMediosPago(userDetails.getUsername()));
    }

    @PostMapping("/medios-pago")
    public ResponseEntity<MedioPagoResponse> agregarMedioPago(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MedioPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.agregarMedioPago(userDetails.getUsername(), request));
    }

    @DeleteMapping("/medios-pago/{id}")
    public ResponseEntity<Map<String, String>> eliminarMedioPago(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        usuarioService.eliminarMedioPago(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Medio de pago eliminado"));
    }

    @GetMapping("/multas")
    public ResponseEntity<List<MultaResponse>> listarMultas(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(multaService.listarMultas(userDetails.getUsername()));
    }

    @PostMapping("/multas/{id}/pagar")
    public ResponseEntity<MultaResponse> pagarMulta(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PagarMultaRequest request) {
        return ResponseEntity.ok(multaService.pagarMulta(id, userDetails.getUsername(), request));
    }

    @GetMapping("/compras/{compraId}")
    public ResponseEntity<CompraResponse> obtenerCompra(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long compraId) {
        return ResponseEntity.ok(compraService.obtenerCompra(compraId, userDetails.getUsername()));
    }

    @GetMapping("/participaciones")
    public ResponseEntity<List<Map<String, Object>>> listarParticipaciones(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        // TODO: implementar filtros completos
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/metricas")
    public ResponseEntity<Map<String, Object>> obtenerMetricas(
            @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: implementar métricas agregadas
        return ResponseEntity.ok(Map.of(
                "total_subastas_asistidas", 0,
                "total_ganadas", 0,
                "total_ofertado", 0,
                "total_pagado", 0
        ));
    }
}
