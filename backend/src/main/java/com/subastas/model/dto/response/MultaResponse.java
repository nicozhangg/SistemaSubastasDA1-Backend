package com.subastas.model.dto.response;

import com.subastas.model.enums.EstadoMulta;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MultaResponse {
    private Long multaId;
    private BigDecimal monto;
    private String motivo;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaLimitePago;
    private EstadoMulta estado;
    private Boolean puedeParticiparNuevamente;
}
