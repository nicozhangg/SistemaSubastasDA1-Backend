package com.subastas.model.dto.response;

import com.subastas.model.enums.EstadoItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ItemResponse {
    private Long itemId;
    private String numeroPieza;
    private String descripcion;
    private BigDecimal precioBase;
    private EstadoItem estado;
    private String duenioActual;
    private List<ImagenInfo> imagenes;
    private List<String> componentes;
    private boolean esObraArte;
    private String artista;
    private LocalDate fechaCreacion;
    private String historia;
    private String ubicacionFisica;
    private BigDecimal mejorOferta;
    private BigDecimal pujaMinima;
    private BigDecimal pujaMaxima;
    private SubastaInfo subasta;
    private PolizaInfo poliza;

    @Data
    @Builder
    public static class ImagenInfo {
        private Long imagenId;
        private String url;
        private int orden;
        private String descripcion;
    }

    @Data
    @Builder
    public static class SubastaInfo {
        private Long id;
        private String titulo;
        private String ubicacion;
    }

    @Data
    @Builder
    public static class PolizaInfo {
        private Long polizaId;
        private String aseguradoraNombre;
        private String aseguradoraContacto;
        private BigDecimal valorAsegurado;
        private LocalDate vigenciaDesde;
        private LocalDate vigenciaHasta;
    }
}
