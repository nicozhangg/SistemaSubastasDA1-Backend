package com.subastas.model.enums;

/**
 * Categoría del usuario y de la subasta. El ordinal define la jerarquía de acceso:
 * un usuario puede entrar a subastas de su categoría o inferior.
 * ORO y PLATINO no tienen límites de puja (pueden ofertar cualquier monto por encima
 * de la mejor oferta actual).
 */
public enum Categoria {
    COMUN, ESPECIAL, PLATA, ORO, PLATINO;

    /** Devuelve true si este usuario puede acceder a una subasta de la categoría dada. */
    public boolean puedeAcceder(Categoria categoriaSubasta) {
        return this.ordinal() >= categoriaSubasta.ordinal();
    }

    /** Devuelve true para categorías premium que no tienen restricción de rango en sus pujas. */
    public boolean sinLimitesPuja() {
        return this == ORO || this == PLATINO;
    }
}
