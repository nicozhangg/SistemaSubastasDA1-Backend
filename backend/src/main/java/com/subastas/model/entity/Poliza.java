package com.subastas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "polizas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aseguradora_nombre", nullable = false)
    private String aseguradoraNombre;

    @Column(name = "aseguradora_contacto")
    private String aseguradoraContacto;

    @Column(name = "valor_asegurado", precision = 15, scale = 2)
    private BigDecimal valorAsegurado;

    @Column(precision = 15, scale = 2)
    private BigDecimal prima;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @ElementCollection
    @CollectionTable(name = "poliza_bienes", joinColumns = @JoinColumn(name = "poliza_id"))
    @Column(name = "bien")
    @Builder.Default
    private List<String> bienesCubiertos = new ArrayList<>();
}
