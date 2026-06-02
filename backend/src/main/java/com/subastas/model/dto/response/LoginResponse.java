package com.subastas.model.dto.response;

import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoUsuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String tokenAcceso;
    private UsuarioInfo usuario;

    @Data
    @Builder
    public static class UsuarioInfo {
        private Long id;
        private String nombre;
        private String apellido;
        private String email;
        private Categoria categoria;
        private EstadoUsuario estado;
    }
}
