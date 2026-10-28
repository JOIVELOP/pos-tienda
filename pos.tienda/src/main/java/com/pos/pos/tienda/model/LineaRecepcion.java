package com.pos.pos.tienda.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "T_LINEA_RECEPCION")
public class LineaRecepcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recepcion", nullable = false)
    private RecepcionCompra recepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_linea_compra", nullable = false)
    private LineaCompra lineaCompra;

    @Column(name = "cantidad_buena", nullable = false)
    private Integer cantidadBuena;

    @Column(name = "cantidad_danada", nullable = false)
    private Integer cantidadDanada;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(length = 255)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RecepcionCompra getRecepcion() { return recepcion; }
    public void setRecepcion(RecepcionCompra recepcion) { this.recepcion = recepcion; }
    public LineaCompra getLineaCompra() { return lineaCompra; }
    public void setLineaCompra(LineaCompra lineaCompra) { this.lineaCompra = lineaCompra; }
    public Integer getCantidadBuena() { return cantidadBuena; }
    public void setCantidadBuena(Integer cantidadBuena) { this.cantidadBuena = cantidadBuena; }
    public Integer getCantidadDanada() { return cantidadDanada; }
    public void setCantidadDanada(Integer cantidadDanada) { this.cantidadDanada = cantidadDanada; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
