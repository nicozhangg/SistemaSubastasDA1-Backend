package com.subastas.util;

import com.subastas.model.entity.Usuario;

public final class AliasUtil {

    private AliasUtil() {}

    public static String generarAlias(Usuario usuario) {
        if (usuario == null) return null;
        if (usuario.getId() == null) return "postor_0000";
        return "postor_" + String.format("%04x", Math.abs(usuario.getId().hashCode() % 0xFFFF));
    }
}
