package com.subastas.model.dto.websocket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BidUpdatedMessage {
    @Builder.Default
    private final String tipo = "BID_UPDATED";
    private Long itemId;
    private BigDecimal nuevaMejorOferta;
    private String mejorPostorAlias;
    private BigDecimal pujaMinima;
    private BigDecimal pujaMaxima;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
