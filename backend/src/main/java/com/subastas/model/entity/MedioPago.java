package com.subastas.model.entity;

import com.subastas.model.enums.Moneda;
import com.subastas.model.enums.TipoMedioPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medios_pago")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMedioPago tipo;

    @Column(nullable = false)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Moneda moneda;

    @Column(nullable = false)
    @Builder.Default
    private boolean verificado = false;

    // Para CHEQUE_CERTIFICADO
    @Column(name = "monto_limite", precision = 15, scale = 2)
    private BigDecimal montoLimite;

    // Para CUENTA_BANCARIA
    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    private String banco;

    // nacional / extranjera
    @Column(name = "tipo_cuenta")
    private String tipoCuenta;

    private String cbu;

    // Para TARJETA_CREDITO
    @Column(name = "numero_tarjeta")
    private String numeroTarjeta;

    private String titular;
    private String vencimiento;

    // nacional / extranjera
    @Column(name = "tipo_tarjeta")
    private String tipoTarjeta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
