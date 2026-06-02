package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UbicacionResponse {
    private String depositoNombre;
    private String depositoDireccion;
    private Double latitud;
    private Double longitud;
    private LocalDateTime fechaIngreso;
    private String estadoFisico;
}
