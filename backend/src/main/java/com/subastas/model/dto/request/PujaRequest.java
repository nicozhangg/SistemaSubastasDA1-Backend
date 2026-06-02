package com.subastas.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PujaRequest {

    @NotNull(message = "El item es obligatorio")
    @Positive(message = "El item debe ser un ID válido")
    private Long itemId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    private BigDecimal monto;

    @NotNull(message = "El medio de pago es obligatorio")
    @Positive(message = "El medio de pago debe ser un ID válido")
    private Long medioPagoId;
}
