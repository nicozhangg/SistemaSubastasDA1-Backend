package com.subastas.model.dto.websocket;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PujaWebSocketRequest {
    private Long itemId;
    private BigDecimal monto;
    private Long medioPagoId;
}
