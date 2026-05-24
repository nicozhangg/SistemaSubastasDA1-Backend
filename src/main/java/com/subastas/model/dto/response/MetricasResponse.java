package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MetricasResponse {
    private int totalSubastasAsistidas;
    private int totalGanadas;
    private BigDecimal totalPagado;
    private int multasPendientes;
}
