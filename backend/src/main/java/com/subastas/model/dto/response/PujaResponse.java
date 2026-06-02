package com.subastas.model.dto.response;

import com.subastas.model.enums.EstadoPuja;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PujaResponse {
    private Long pujaId;
    private BigDecimal monto;
    private EstadoPuja estado;
    private LocalDateTime timestamp;
    private BigDecimal nuevaMejorOferta;
    private String postorAlias;
}
