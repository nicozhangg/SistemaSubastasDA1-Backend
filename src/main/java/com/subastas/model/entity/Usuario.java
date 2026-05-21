package com.subastas.model.entity;

import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Postor del sistema. La categoría determina a qué subastas puede acceder
 * y si tiene límites en los montos de puja (ORO y PLATINO no los tienen).
 * El campo multasPendientes se mantiene sincronizado con la tabla de multas
 * para evitar consultas adicionales en cada validación de puja.
 */
@Entity
@Table(name = "usuarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(name = "domicilio_legal")
    private String domicilioLegal;

    @Column(name = "pais_origen")
    private String paisOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoUsuario estado;

    @Column(name = "foto_dni_frente")
    private String fotoDniFrente;

    @Column(name = "foto_dni_dorso")
    private String fotoDniDorso;

    @Column(name = "multas_pendientes", nullable = false)
    @Builder.Default
    private int multasPendientes = 0;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "token_email")
    private String tokenEmail;

    @Column(name = "token_expiracion")
    private LocalDateTime tokenExpiracion;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MedioPago> mediosPago = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Participacion> participaciones = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Consignacion> consignaciones = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (multasPendientes < 0) {
            multasPendientes = 0;
        }
    }
}
