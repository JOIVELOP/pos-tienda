package com.pos.pos.tienda.dto;

import com.pos.pos.tienda.model.Venta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DevolucionDTO {
    private Long id;
    private String folio;
    private LocalDateTime fecha;
    private String folioVenta;
    private String motivo;
    private Venta.MetodoPago metodoReembolso;
    private BigDecimal totalReembolsado;
    private String usuario;
    private List<LineaDTO> lineas;

    public static class CrearRequest {
        @NotNull
        private Long idVenta;
        @NotBlank @Size(max = 255)
        private String motivo;
        @NotNull
        private Venta.MetodoPago metodoReembolso;
        @NotEmpty @Valid
        private List<LineaRequest> lineas;
        public Long getIdVenta() { return idVenta; }
        public void setIdVenta(Long idVenta) { this.idVenta = idVenta; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public Venta.MetodoPago getMetodoReembolso() { return metodoReembolso; }
        public void setMetodoReembolso(Venta.MetodoPago metodoReembolso) { this.metodoReembolso = metodoReembolso; }
        public List<LineaRequest> getLineas() { return lineas; }
        public void setLineas(List<LineaRequest> lineas) { this.lineas = lineas; }
    }

    public static class LineaRequest {
        @NotNull
        private Long idDetalleVenta;
        @NotNull @Min(1)
        private Integer cantidad;
        @NotNull
        private Boolean reingresaInventario;
        public Long getIdDetalleVenta() { return idDetalleVenta; }
        public void setIdDetalleVenta(Long idDetalleVenta) { this.idDetalleVenta = idDetalleVenta; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public Boolean getReingresaInventario() { return reingresaInventario; }
        public void setReingresaInventario(Boolean reingresaInventario) { this.reingresaInventario = reingresaInventario; }
    }

    public static class LineaDTO {
        private Long idDetalleVenta;
        private String producto;
        private Integer cantidad;
        private Boolean reingresaInventario;
        private BigDecimal montoReembolsado;
        public Long getIdDetalleVenta() { return idDetalleVenta; }
        public void setIdDetalleVenta(Long idDetalleVenta) { this.idDetalleVenta = idDetalleVenta; }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public Boolean getReingresaInventario() { return reingresaInventario; }
        public void setReingresaInventario(Boolean reingresaInventario) { this.reingresaInventario = reingresaInventario; }
        public BigDecimal getMontoReembolsado() { return montoReembolsado; }
        public void setMontoReembolsado(BigDecimal montoReembolsado) { this.montoReembolsado = montoReembolsado; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getFolioVenta() { return folioVenta; }
    public void setFolioVenta(String folioVenta) { this.folioVenta = folioVenta; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Venta.MetodoPago getMetodoReembolso() { return metodoReembolso; }
    public void setMetodoReembolso(Venta.MetodoPago metodoReembolso) { this.metodoReembolso = metodoReembolso; }
    public BigDecimal getTotalReembolsado() { return totalReembolsado; }
    public void setTotalReembolsado(BigDecimal totalReembolsado) { this.totalReembolsado = totalReembolsado; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public List<LineaDTO> getLineas() { return lineas; }
    public void setLineas(List<LineaDTO> lineas) { this.lineas = lineas; }
}
