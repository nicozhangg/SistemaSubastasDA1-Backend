package com.subastas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participaciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta_id", nullable = false)
    private Subasta subasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_pago_id")
    private MedioPago medioPago;

    @Column(nullable = false)
    @Builder.Default
    private boolean conectado = false;

    @Column(name = "fecha_conexion")
    private LocalDateTime fechaConexion;

    @Column(name = "fecha_desconexion")
    private LocalDateTime fechaDesconexion;
}
