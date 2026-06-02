package com.subastas.model.dto.request;

import com.subastas.model.enums.Moneda;
import com.subastas.model.enums.TipoMedioPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedioPagoRequest {

    @NotNull(message = "El tipo es obligatorio")
    private TipoMedioPago tipo;

    @NotBlank(message = "El alias es obligatorio")
    private String alias;

    @NotNull(message = "La moneda es obligatoria")
    private Moneda moneda;

    // CHEQUE_CERTIFICADO
    private BigDecimal montoCheque;

    // CUENTA_BANCARIA
    private String numeroCuenta;
    private String banco;
    private String tipoCuenta;
    private String cbu;

    // TARJETA_CREDITO
    private String numeroTarjeta;
    private String titular;
    private String vencimiento;
    private String tipoTarjeta;
}
