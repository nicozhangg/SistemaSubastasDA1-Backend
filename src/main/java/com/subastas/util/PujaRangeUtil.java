package com.subastas.util;

import com.subastas.model.entity.Item;
import com.subastas.model.entity.Subasta;

import java.math.BigDecimal;

/** Cálculo centralizado del rango de puja mínimo/máximo para un ítem. */
public final class PujaRangeUtil {

    private static final BigDecimal FACTOR_MIN = new BigDecimal("0.01");
    private static final BigDecimal FACTOR_MAX = new BigDecimal("0.20");

    private PujaRangeUtil() {}

    public static BigDecimal calcularMinima(Item item, Subasta subasta) {
        if (subasta == null || subasta.getCategoria().sinLimitesPuja()) return null;
        if (item.getPrecioBase() == null) return null;
        return baseEfectiva(item).add(item.getPrecioBase().multiply(FACTOR_MIN));
    }

    public static BigDecimal calcularMaxima(Item item, Subasta subasta) {
        if (subasta == null || subasta.getCategoria().sinLimitesPuja()) return null;
        if (item.getPrecioBase() == null) return null;
        return baseEfectiva(item).add(item.getPrecioBase().multiply(FACTOR_MAX));
    }

    private static BigDecimal baseEfectiva(Item item) {
        return item.getMejorOferta() != null ? item.getMejorOferta() : item.getPrecioBase();
    }
}
