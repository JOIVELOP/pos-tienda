package com.pos.pos.tienda.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "T_LINEA_DEVOLUCION")
public class LineaDevolucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_devolucion", nullable = false)
    private DevolucionVenta devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_venta", nullable = false)
    private DetalleVenta detalleVenta;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "reingresa_inventario", nullable = false)
    private Boolean reingresaInventario;

    @Column(name = "monto_reembolsado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoReembolsado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DevolucionVenta getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionVenta devolucion) { this.devolucion = devolucion; }
    public DetalleVenta getDetalleVenta() { return detalleVenta; }
    public void setDetalleVenta(DetalleVenta detalleVenta) { this.detalleVenta = detalleVenta; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Boolean getReingresaInventario() { return reingresaInventario; }
    public void setReingresaInventario(Boolean reingresaInventario) { this.reingresaInventario = reingresaInventario; }
    public BigDecimal getMontoReembolsado() { return montoReembolsado; }
    public void setMontoReembolsado(BigDecimal montoReembolsado) { this.montoReembolsado = montoReembolsado; }
}
