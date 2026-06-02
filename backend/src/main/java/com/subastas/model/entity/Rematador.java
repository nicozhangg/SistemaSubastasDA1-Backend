package com.subastas.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rematadores")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Rematador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String email;

    private String telefono;
    private String matricula;
}
