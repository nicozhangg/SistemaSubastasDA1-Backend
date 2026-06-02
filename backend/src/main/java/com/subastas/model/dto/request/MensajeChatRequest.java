package com.subastas.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MensajeChatRequest {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String contenido;
}
