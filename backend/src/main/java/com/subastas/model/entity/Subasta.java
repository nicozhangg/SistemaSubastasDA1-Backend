package com.subastas.model.entity;

import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evento de subasta. La categoría restringe qué usuarios pueden acceder;
 * la moneda aplica a todos los ítems del lote. El rematador es opcional
 * y puede asignarse después de crear la subasta.
 */
@Entity
@Table(name = "subastas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoSubasta estado = EstadoSubasta.PROXIMA;

    private String ubicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rematador_id")
    private Rematador rematador;

    @OneToMany(mappedBy = "subasta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();
}
