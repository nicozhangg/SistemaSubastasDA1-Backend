package com.subastas.util;

import com.subastas.model.entity.Usuario;

public final class AliasUtil {

    private AliasUtil() {}

    public static String generarAlias(Usuario usuario) {
        if (usuario == null) return null;
        String nombre = usuario.getNombre();
        if (nombre == null || nombre.isBlank()) return "postor_***";
        return "postor_" + nombre.substring(0, Math.min(3, nombre.length())) + "***";
    }
}
