package com.subastas.model.entity;

import com.subastas.model.enums.EstadoMulta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "multas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "fecha_generacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @Column(name = "fecha_limite_pago")
    private LocalDateTime fechaLimitePago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoMulta estado = EstadoMulta.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puja_id")
    private Puja puja;
}
