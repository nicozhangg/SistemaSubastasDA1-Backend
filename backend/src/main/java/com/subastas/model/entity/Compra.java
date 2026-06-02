package com.subastas.model.entity;

import com.subastas.model.enums.EstadoPago;
import com.subastas.model.enums.ModalidadEntrega;
import com.subastas.model.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // nullable: la empresa compra el ítem cuando nadie puja (usuario = null en ese caso)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "monto_ofertado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoOfertado;

    @Column(precision = 15, scale = 2)
    private BigDecimal comisiones;

    @Column(name = "costo_envio", precision = 15, scale = 2)
    private BigDecimal costoEnvio;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Moneda moneda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_pago_id")
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    @Builder.Default
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(name = "direccion_envio")
    private String direccionEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega")
    private ModalidadEntrega modalidadEntrega;

    @Column(name = "cobertura_seguro_activa", nullable = false)
    @Builder.Default
    private boolean coberturaSeguroActiva = true;

    @Column(name = "fecha_limite_pago")
    private LocalDateTime fechaLimitePago;
}
