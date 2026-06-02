package com.subastas.model.dto.response;

import com.subastas.model.enums.EstadoConsignacion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConsignacionResponse {
    private Long consignacionId;
    private String descripcion;
    private String datosAdicionales;
    private EstadoConsignacion estado;
    private boolean aceptaPertenencia;
    private String motivoRechazo;
    private BigDecimal precioSugerido;
    private BigDecimal valorBase;
    private BigDecimal comisiones;
    private Long subastaId;
    private List<String> fotosUrls;
    private String mensaje;
    // Para aceptar condiciones
    private LocalDateTime fechaSubasta;
    // Para rechazar condiciones
    private BigDecimal gastosEstimados;
}
