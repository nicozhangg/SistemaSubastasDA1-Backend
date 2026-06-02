package com.subastas.model.dto.websocket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BidConfirmedMessage {
    @Builder.Default
    private final String tipo = "BID_CONFIRMED";
    private Long pujaId;
    private BigDecimal monto;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
