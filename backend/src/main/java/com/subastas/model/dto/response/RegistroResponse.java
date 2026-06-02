package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistroResponse {
    private Long usuarioId;
    private String estado;
    private String mensaje;
    // Solo presente en paso 2
    private String email;
    private String tokenAcceso;
    private String categoria;
}
