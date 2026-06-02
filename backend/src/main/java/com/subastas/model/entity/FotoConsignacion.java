package com.subastas.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fotos_consignacion")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FotoConsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    private int orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignacion_id", nullable = false)
    private Consignacion consignacion;
}
