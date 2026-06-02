package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class MetricasResponse {
    private long totalSubastasAsistidas;
    private long totalGanadas;
    private BigDecimal totalOfertado;
    private BigDecimal totalPagado;
    private double porcentajeVictorias;
    private String categoriaMasParticipada;
    private Map<String, Long> subastasPorCategoria;
}
