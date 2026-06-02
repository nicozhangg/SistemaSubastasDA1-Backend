package com.subastas.controller;

import com.subastas.model.dto.request.EntregaRequest;
import com.subastas.model.dto.request.MensajeChatRequest;
import com.subastas.model.dto.response.CompraResponse;
import com.subastas.model.dto.response.MensajeChatResponse;
import com.subastas.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chat post-subasta vinculado a una compra.
 * El ganador coordina con la empresa la modalidad de entrega y el seguro.
 */
@RestController
@RequestMapping("/api/v1/compras/{compraId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat")
    public ResponseEntity<List<MensajeChatResponse>> obtenerMensajes(
            @PathVariable Long compraId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.obtenerMensajes(compraId, userDetails.getUsername()));
    }

    @PostMapping("/chat")
    public ResponseEntity<MensajeChatResponse> enviarMensaje(
            @PathVariable Long compraId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MensajeChatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.enviarMensaje(compraId, userDetails.getUsername(), request));
    }

    @PatchMapping("/entrega")
    public ResponseEntity<CompraResponse> confirmarEntrega(
            @PathVariable Long compraId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.ok(chatService.confirmarEntrega(compraId, userDetails.getUsername(), request));
    }
}
