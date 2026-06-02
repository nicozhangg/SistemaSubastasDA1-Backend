package com.subastas.model.dto.response;

import com.subastas.model.enums.Moneda;
import com.subastas.model.enums.TipoMedioPago;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MedioPagoResponse {
    private Long id;
    private TipoMedioPago tipo;
    private String alias;
    private Moneda moneda;
    private boolean verificado;
    private BigDecimal montoLimite;
    private String mensaje;
}
