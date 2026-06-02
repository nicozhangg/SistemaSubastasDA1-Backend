package com.subastas.model.dto.websocket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AuctionClosedMessage {
    @Builder.Default
    private final String tipo = "AUCTION_CLOSED";
    private Long itemId;
    private String ganadorAlias;
    private BigDecimal montoFinal;
}
