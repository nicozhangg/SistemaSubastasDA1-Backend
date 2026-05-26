package com.subastas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ComisionService {

    @Value("${app.comision.porcentaje:10}")
    private BigDecimal porcentajeComision;

    @Value("${app.envio.costo-fijo:1500}")
    private BigDecimal costoEnvioFijo;

    public BigDecimal calcularComision(BigDecimal montoOfertado) {
        return montoOfertado.multiply(porcentajeComision)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularCostoEnvio() {
        return costoEnvioFijo;
    }

    public BigDecimal calcularTotal(BigDecimal montoOfertado) {
        return montoOfertado.add(calcularComision(montoOfertado)).add(calcularCostoEnvio());
    }
}
