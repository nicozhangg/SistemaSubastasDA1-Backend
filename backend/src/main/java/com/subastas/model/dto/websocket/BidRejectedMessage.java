package com.subastas.model.dto.websocket;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BidRejectedMessage {
    @Builder.Default
    private final String tipo = "BID_REJECTED";
    private String motivo;
    private String mensaje;
}
