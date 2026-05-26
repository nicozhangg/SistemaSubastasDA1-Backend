package com.subastas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ComisionServiceTest {

    private ComisionService service;

    @BeforeEach
    void setUp() {
        service = new ComisionService();
        ReflectionTestUtils.setField(service, "porcentajeComision", new BigDecimal("10"));
        ReflectionTestUtils.setField(service, "costoEnvioFijo", new BigDecimal("1500"));
    }

    @Test
    void calcular_comision_es_10_porciento_del_monto() {
        assertThat(service.calcularComision(new BigDecimal("100000.00")))
                .isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void calcular_comision_redondea_mitad_arriba() {
        // 10% de 10001 = 1000.10 → sin redondeo adicional
        assertThat(service.calcularComision(new BigDecimal("10001.00")))
                .isEqualByComparingTo(new BigDecimal("1000.10"));
    }

    @Test
    void calcular_costo_envio_devuelve_valor_configurado() {
        assertThat(service.calcularCostoEnvio())
                .isEqualByComparingTo(new BigDecimal("1500"));
    }

    @Test
    void calcular_total_suma_monto_comision_y_envio() {
        // 50000 + 5000 (10%) + 1500 = 56500
        assertThat(service.calcularTotal(new BigDecimal("50000.00")))
                .isEqualByComparingTo(new BigDecimal("56500.00"));
    }

    @Test
    void calcular_comision_de_cero_es_cero() {
        assertThat(service.calcularComision(BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calcular_total_de_cero_solo_incluye_costo_envio() {
        // 0 + 0 (comision) + 1500 = 1500
        assertThat(service.calcularTotal(BigDecimal.ZERO))
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }
}
