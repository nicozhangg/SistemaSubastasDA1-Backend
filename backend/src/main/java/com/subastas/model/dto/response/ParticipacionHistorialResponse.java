package com.subastas.model.dto.response;

import com.subastas.model.enums.Categoria;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ParticipacionHistorialResponse {
    private Long subastaId;
    private String titulo;
    private LocalDateTime fecha;
    private Categoria categoria;
    private long itemsPujados;
    private long itemsGanados;
    private BigDecimal montoTotalOfertado;
    /** ganada | perdida | en_curso */
    private String resultado;
}
