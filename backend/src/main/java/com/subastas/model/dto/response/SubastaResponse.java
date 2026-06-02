package com.subastas.model.dto.response;

import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubastaResponse {
    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private Categoria categoria;
    private Moneda moneda;
    private EstadoSubasta estado;
    private String ubicacion;
    private RematadorInfo rematador;
    private int totalItems;

    @Data
    @Builder
    public static class RematadorInfo {
        private Long id;
        private String nombre;
        private String apellido;
        private String matricula;
    }
}
