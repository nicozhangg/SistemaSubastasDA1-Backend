package com.subastas.model.dto.request;

import com.subastas.model.enums.ModalidadEntrega;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntregaRequest {

    @NotNull(message = "La modalidad de entrega es obligatoria")
    private ModalidadEntrega modalidadEntrega;

    private String direccionEnvio;
}
