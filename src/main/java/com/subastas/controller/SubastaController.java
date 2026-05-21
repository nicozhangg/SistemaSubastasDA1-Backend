package com.subastas.controller;

import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.request.PujaRequest;
import com.subastas.model.dto.response.ConectarSubastaResponse;
import com.subastas.model.dto.response.EstadoPujaResponse;
import com.subastas.model.dto.response.PujaResponse;
import com.subastas.model.dto.response.SubastaResponse;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import com.subastas.service.PujaService;
import com.subastas.service.SubastaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de subastas: listado paginado, detalle, conexión/desconexión
 * y gestión de pujas (REST). Las pujas también se pueden realizar vía WebSocket
 * a través de {@link PujaWebSocketController}.
 */
@RestController
@RequestMapping("/api/v1/subastas")
@RequiredArgsConstructor
public class SubastaController {

    private final SubastaService subastaService;
    private final PujaService pujaService;

    /** Devuelve solo las subastas accesibles según la categoría del usuario autenticado. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) EstadoSubasta estado,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) Moneda moneda,
            @RequestParam(defaultValue = "0") int page) {

        Page<SubastaResponse> result = subastaService.listar(estado, categoria, moneda, page, userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "data", result.getContent(),
                "total", result.getTotalElements(),
                "page", result.getNumber()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubastaResponse> obtener(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(subastaService.obtener(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/conectar")
    public ResponseEntity<ConectarSubastaResponse> conectar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ConectarSubastaRequest request) {
        return ResponseEntity.ok(subastaService.conectar(id, userDetails.getUsername(), request));
    }

    @PostMapping("/{id}/desconectar")
    public ResponseEntity<Map<String, String>> desconectar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        subastaService.desconectar(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("mensaje", "Desconectado de la subasta"));
    }

    @GetMapping("/{id}/pujas/estado")
    public ResponseEntity<EstadoPujaResponse> obtenerEstadoPuja(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(subastaService.obtenerEstadoPuja(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/pujas")
    public ResponseEntity<PujaResponse> realizarPuja(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PujaRequest request) {
        PujaResponse response = pujaService.realizarPuja(id, userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/pujas/historial")
    public ResponseEntity<List<PujaResponse>> obtenerHistorial(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Boolean soloPropias) {
        return ResponseEntity.ok(pujaService.obtenerHistorial(id, itemId, soloPropias, userDetails.getUsername()));
    }
}
