package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UbicacionResponse {
    private String depositoNombre;
    private String depositoDireccion;
    private Double lat;
    private Double lng;
    private LocalDateTime fechaIngreso;
    private String estadoFisico;
}
