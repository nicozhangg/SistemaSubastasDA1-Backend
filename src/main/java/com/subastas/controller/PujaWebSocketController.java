package com.subastas.controller;

import com.subastas.exception.BusinessException;
import com.subastas.model.dto.request.PujaRequest;
import com.subastas.model.dto.websocket.BidRejectedMessage;
import com.subastas.model.dto.websocket.PujaWebSocketRequest;
import com.subastas.service.PujaService;
import com.subastas.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Controlador WebSocket (STOMP) para pujas en tiempo real.
 * Los clientes envían pujas a /app/subastas/{id}/pujar y reciben
 * confirmación o rechazo de forma privada en /user/queue/pujas,
 * mientras el broadcast de la nueva mejor oferta va a /topic/subastas/{id}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PujaWebSocketController {

    private final PujaService pujaService;
    private final WebSocketService webSocketService;

    @MessageMapping("/subastas/{subastaId}/pujar")
    public void pujar(@DestinationVariable Long subastaId,
                      @Payload PujaWebSocketRequest wsRequest,
                      Principal principal) {
        if (principal == null) {
            log.warn("Intento de puja sin autenticación en subasta {}", subastaId);
            return;
        }

        String email = principal.getName();
        log.debug("Puja WebSocket recibida de {} para subasta {}: {}", email, subastaId, wsRequest);

        if (wsRequest.getItemId() == null || wsRequest.getMonto() == null
                || wsRequest.getMedioPagoId() == null
                || wsRequest.getMonto().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
                    .motivo("VALIDACION")
                    .mensaje("Datos de puja incompletos o inválidos")
                    .build());
            return;
        }

        try {
            PujaRequest request = new PujaRequest();
            request.setItemId(wsRequest.getItemId());
            request.setMonto(wsRequest.getMonto());
            request.setMedioPagoId(wsRequest.getMedioPagoId());

            pujaService.realizarPuja(subastaId, email, request);

        } catch (BusinessException e) {
            webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
                    .motivo(e.getCodigo())
                    .mensaje(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error inesperado procesando puja WebSocket de {} en subasta {}: {}", email, subastaId, e.getMessage(), e);
            webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
                    .motivo("ERROR_INTERNO")
                    .mensaje("No se pudo procesar la puja")
                    .build());
        }
    }
}
