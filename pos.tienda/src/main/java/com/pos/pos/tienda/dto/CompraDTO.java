package com.pos.pos.tienda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CompraDTO {
    private Long id;
    private String folio;
    private String estado;
    private LocalDateTime fecha;
    private Long idProveedor;
    private String nombreProveedor;
    private String usuario;
    private List<LineaDTO> lineas;
    private List<RecepcionDTO> recepciones;

    public static class CrearRequest {
        @NotNull
        private Long idProveedor;
        @NotEmpty @Valid
        private List<LineaRequest> lineas;
        public Long getIdProveedor() { return idProveedor; }
        public void setIdProveedor(Long idProveedor) { this.idProveedor = idProveedor; }
        public List<LineaRequest> getLineas() { return lineas; }
        public void setLineas(List<LineaRequest> lineas) { this.lineas = lineas; }
    }

    public static class LineaRequest {
        @NotNull
        private Long idProducto;
        @NotNull @Min(1)
        private Integer cantidadSolicitada;
        @NotNull @DecimalMin("0.0")
        private BigDecimal costoUnitario;
        public Long getIdProducto() { return idProducto; }
        public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
        public Integer getCantidadSolicitada() { return cantidadSolicitada; }
        public void setCantidadSolicitada(Integer cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
        public BigDecimal getCostoUnitario() { return costoUnitario; }
        public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    }

    public static class RecepcionRequest {
        @NotEmpty @Valid
        private List<LineaRecepcionRequest> lineas;
        public List<LineaRecepcionRequest> getLineas() { return lineas; }
        public void setLineas(List<LineaRecepcionRequest> lineas) { this.lineas = lineas; }
    }

    public static class LineaRecepcionRequest {
        @NotNull
        private Long idLineaCompra;
        @NotNull @Min(0)
        private Integer cantidadBuena;
        @NotNull @Min(0)
        private Integer cantidadDanada;
        @NotNull @DecimalMin("0.0")
        private BigDecimal costoUnitario;
        @Size(max = 255)
        private String observacion;
        public Long getIdLineaCompra() { return idLineaCompra; }
        public void setIdLineaCompra(Long idLineaCompra) { this.idLineaCompra = idLineaCompra; }
        public Integer getCantidadBuena() { return cantidadBuena; }
        public void setCantidadBuena(Integer cantidadBuena) { this.cantidadBuena = cantidadBuena; }
        public Integer getCantidadDanada() { return cantidadDanada; }
        public void setCantidadDanada(Integer cantidadDanada) { this.cantidadDanada = cantidadDanada; }
        public BigDecimal getCostoUnitario() { return costoUnitario; }
        public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
        public String getObservacion() { return observacion; }
        public void setObservacion(String observacion) { this.observacion = observacion; }
    }

    public static class LineaDTO {
        private Long id;
        private Long idProducto;
        private String codigoProducto;
        private String nombreProducto;
        private Integer cantidadSolicitada;
        private Integer cantidadRecibida;
        private BigDecimal costoUnitario;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getIdProducto() { return idProducto; }
        public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
        public String getCodigoProducto() { return codigoProducto; }
        public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }
        public String getNombreProducto() { return nombreProducto; }
        public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
        public Integer getCantidadSolicitada() { return cantidadSolicitada; }
        public void setCantidadSolicitada(Integer cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
        public Integer getCantidadRecibida() { return cantidadRecibida; }
        public void setCantidadRecibida(Integer cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }
        public BigDecimal getCostoUnitario() { return costoUnitario; }
        public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    }

    public static class RecepcionDTO {
        private Long id;
        private String folio;
        private LocalDateTime fecha;
        private String usuario;
        private List<LineaRecepcionDTO> lineas;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFolio() { return folio; }
        public void setFolio(String folio) { this.folio = folio; }
        public LocalDateTime getFecha() { return fecha; }
        public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public List<LineaRecepcionDTO> getLineas() { return lineas; }
        public void setLineas(List<LineaRecepcionDTO> lineas) { this.lineas = lineas; }
    }

    public static class LineaRecepcionDTO {
        private Long idLineaCompra;
        private String producto;
        private Integer cantidadBuena;
        private Integer cantidadDanada;
        private BigDecimal costoUnitario;
        private String observacion;
        public Long getIdLineaCompra() { return idLineaCompra; }
        public void setIdLineaCompra(Long idLineaCompra) { this.idLineaCompra = idLineaCompra; }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Integer getCantidadBuena() { return cantidadBuena; }
        public void setCantidadBuena(Integer cantidadBuena) { this.cantidadBuena = cantidadBuena; }
        public Integer getCantidadDanada() { return cantidadDanada; }
        public void setCantidadDanada(Integer cantidadDanada) { this.cantidadDanada = cantidadDanada; }
        public BigDecimal getCostoUnitario() { return costoUnitario; }
        public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
        public String getObservacion() { return observacion; }
        public void setObservacion(String observacion) { this.observacion = observacion; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Long getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Long idProveedor) { this.idProveedor = idProveedor; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public List<LineaDTO> getLineas() { return lineas; }
    public void setLineas(List<LineaDTO> lineas) { this.lineas = lineas; }
    public List<RecepcionDTO> getRecepciones() { return recepciones; }
    public void setRecepciones(List<RecepcionDTO> recepciones) { this.recepciones = recepciones; }
}
