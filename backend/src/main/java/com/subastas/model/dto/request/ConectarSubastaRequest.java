package com.subastas.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConectarSubastaRequest {

    @NotNull(message = "El medio de pago es obligatorio")
    private Long medioPagoId;
}
