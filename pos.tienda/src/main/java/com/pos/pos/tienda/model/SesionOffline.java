package com.pos.pos.tienda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "T_SESION_OFFLINE")
@Getter
@Setter
public class SesionOffline {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String dispositivo;
    @OneToOne(optional = false)
    @JoinColumn(name = "id_turno", nullable = false, unique = true)
    private TurnoCaja turno;
    @Column(nullable = false)
    private Instant vence;
    @Column(nullable = false)
    private boolean cerrada;
    @Column(nullable = false)
    private long secuencia;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivo;
    @ElementCollection
    @CollectionTable(name = "T_CATALOGO_OFFLINE", joinColumns = @JoinColumn(name = "id_sesion"))
    private List<Articulo> productos = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    public static class Articulo {
        @Column(name = "id_producto", nullable = false)
        private Long id;
        private String codigo;
        @Column(nullable = false)
        private String nombre;
        @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
        private BigDecimal precioVenta;
        @Column(nullable = false)
        private int stock;
    }
}
