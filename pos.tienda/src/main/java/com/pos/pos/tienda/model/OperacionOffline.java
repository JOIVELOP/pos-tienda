package com.pos.pos.tienda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "T_OPERACION_OFFLINE", uniqueConstraints =
        @UniqueConstraint(columnNames = {"id_sesion", "secuencia"}))
@Getter
@Setter
public class OperacionOffline {
    @Id
    @Column(length = 36)
    private String id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionOffline sesion;
    @Column(nullable = false)
    private long secuencia;
    @Column(nullable = false, length = 64)
    private String huella;
    @Column(nullable = false)
    private Instant captura;
    @Column(nullable = false)
    private Instant sincronizacion;
    @Column(nullable = false, length = 30)
    private String folio;
}
