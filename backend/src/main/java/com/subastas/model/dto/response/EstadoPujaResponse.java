package com.subastas.model.dto.response;

import com.subastas.model.enums.EstadoItem;
import com.subastas.model.enums.Moneda;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EstadoPujaResponse {
    private Long itemId;
    private String descripcion;
    private BigDecimal precioBase;
    private BigDecimal mejorOferta;
    private String mejorPostorAlias;
    private BigDecimal pujaMinima;
    private BigDecimal pujaMaxima;
    private Moneda moneda;
    private EstadoItem estadoItem;
}
