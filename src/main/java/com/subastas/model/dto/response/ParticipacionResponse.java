package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ParticipacionResponse {
    private Long subastaId;
    private String subastaTitulo;
    private LocalDateTime fechaConexion;
    private LocalDateTime fechaDesconexion;
    private boolean conectado;
}
