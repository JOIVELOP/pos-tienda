package com.pos.pos.tienda.dto;

import com.pos.pos.tienda.model.Venta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VentaDTO {
    private Long id;
    private String folio;
    private LocalDateTime fecha;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal subtotal;

    @DecimalMin("0.0")
    private BigDecimal descuento = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal total;

    @NotNull
    private Venta.MetodoPago metodoPago;

    private Venta.Estatus estatus;

    private Long idCliente;
    private String nombreCliente;
    private Long idUsuario;
    private String nombreUsuario;

    @NotEmpty
    @Valid
    private List<DetalleVentaDTO> detalles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Venta.MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(Venta.MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Venta.Estatus getEstatus() {
        return estatus;
    }

    public void setEstatus(Venta.Estatus estatus) {
        this.estatus = estatus;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public List<DetalleVentaDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaDTO> detalles) {
        this.detalles = detalles;
    }
}
