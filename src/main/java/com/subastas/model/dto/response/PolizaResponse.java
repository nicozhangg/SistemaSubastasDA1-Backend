package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PolizaResponse {
    private Long polizaId;
    private String aseguradoraNombre;
    private String aseguradoraContacto;
    private BigDecimal valorAsegurado;
    private BigDecimal prima;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    private List<String> bienesCubiertos;
}
