package com.subastas.model.dto.response;

import com.subastas.model.enums.RemitenteMensaje;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MensajeChatResponse {
    private Long mensajeId;
    private String contenido;
    private LocalDateTime timestamp;
    private RemitenteMensaje remitente;
    private boolean leido;
}
