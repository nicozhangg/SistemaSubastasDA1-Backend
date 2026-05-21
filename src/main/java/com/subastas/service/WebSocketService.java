package com.subastas.service;

import com.subastas.model.dto.websocket.AuctionClosedMessage;
import com.subastas.model.dto.websocket.BidConfirmedMessage;
import com.subastas.model.dto.websocket.BidRejectedMessage;
import com.subastas.model.dto.websocket.BidUpdatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Abstracción sobre SimpMessagingTemplate para el envío de mensajes WebSocket.
 * Separa la lógica de destinos STOMP del resto del servicio de pujas.
 *
 * <p>Destinos:
 * <ul>
 *   <li>/topic/subastas/{id}   – broadcast a todos los postores conectados</li>
 *   <li>/user/queue/pujas      – mensaje privado al postor que pujó</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastBidUpdated(Long subastaId, BidUpdatedMessage message) {
        String destination = "/topic/subastas/" + subastaId;
        log.debug("Broadcasting BID_UPDATED a {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    public void sendBidConfirmed(String emailPostor, BidConfirmedMessage message) {
        log.debug("Enviando BID_CONFIRMED a {}", emailPostor);
        messagingTemplate.convertAndSendToUser(emailPostor, "/queue/pujas", message);
    }

    public void sendBidRejected(String emailPostor, BidRejectedMessage message) {
        log.debug("Enviando BID_REJECTED a {}", emailPostor);
        messagingTemplate.convertAndSendToUser(emailPostor, "/queue/pujas", message);
    }

    public void broadcastAuctionClosed(Long subastaId, AuctionClosedMessage message) {
        String destination = "/topic/subastas/" + subastaId;
        log.debug("Broadcasting AUCTION_CLOSED a {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }
}
