package com.subastas.model.dto.response;

import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoUsuario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private Categoria categoria;
    private EstadoUsuario estado;
    private String domicilioLegal;
    private String paisOrigen;
    private LocalDateTime fechaRegistro;
    private int multasPendientes;
}
