package com.subastas.model.entity;

import com.subastas.model.enums.EstadoConsignacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consignaciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Consignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(name = "datos_adicionales", columnDefinition = "TEXT")
    private String datosAdicionales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoConsignacion estado = EstadoConsignacion.PENDIENTE_REVISION;

    @Column(name = "acepta_pertenencia", nullable = false)
    private boolean aceptaPertenencia;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(name = "precio_sugerido", precision = 15, scale = 2)
    private BigDecimal precioSugerido;

    @Column(name = "valor_base", precision = 15, scale = 2)
    private BigDecimal valorBase;

    @Column(precision = 15, scale = 2)
    private BigDecimal comisiones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta_id")
    private Subasta subastaAsignada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_destino_id")
    private MedioPago cuentaDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "consignacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FotoConsignacion> fotos = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "poliza_id")
    private Poliza poliza;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;
}
