package com.pos.pos.tienda.dto;

import com.pos.pos.tienda.model.MovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TurnoCajaDTO {
    private Long id;
    private String folio;
    private String estado;
    private LocalDateTime apertura;
    private LocalDateTime cierre;
    private String usuarioApertura;
    private String usuarioCierre;
    private BigDecimal fondoInicial;
    private BigDecimal ventasEfectivo;
    private BigDecimal entradas;
    private BigDecimal salidas;
    private BigDecimal efectivoEsperado;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;
    private List<MovimientoDTO> movimientos;

    public static class AperturaRequest {
        @NotNull @DecimalMin("0.0")
        private BigDecimal fondoInicial;
        public BigDecimal getFondoInicial() { return fondoInicial; }
        public void setFondoInicial(BigDecimal fondoInicial) { this.fondoInicial = fondoInicial; }
    }

    public static class CierreRequest {
        @NotNull @DecimalMin("0.0")
        private BigDecimal efectivoContado;
        public BigDecimal getEfectivoContado() { return efectivoContado; }
        public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    }

    public static class MovimientoRequest {
        @NotNull
        private MovimientoCaja.Tipo tipo;
        @NotBlank @Size(max = 20)
        private String concepto;
        @NotBlank @Size(max = 255)
        private String descripcion;
        @NotNull @DecimalMin(value = "0.01")
        private BigDecimal monto;
        public MovimientoCaja.Tipo getTipo() { return tipo; }
        public void setTipo(MovimientoCaja.Tipo tipo) { this.tipo = tipo; }
        public String getConcepto() { return concepto; }
        public void setConcepto(String concepto) { this.concepto = concepto; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
    }

    public static class MovimientoDTO {
        private Long id;
        private MovimientoCaja.Tipo tipo;
        private String concepto;
        private String descripcion;
        private BigDecimal monto;
        private LocalDateTime fecha;
        private String usuario;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public MovimientoCaja.Tipo getTipo() { return tipo; }
        public void setTipo(MovimientoCaja.Tipo tipo) { this.tipo = tipo; }
        public String getConcepto() { return concepto; }
        public void setConcepto(String concepto) { this.concepto = concepto; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
        public LocalDateTime getFecha() { return fecha; }
        public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getApertura() { return apertura; }
    public void setApertura(LocalDateTime apertura) { this.apertura = apertura; }
    public LocalDateTime getCierre() { return cierre; }
    public void setCierre(LocalDateTime cierre) { this.cierre = cierre; }
    public String getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(String usuarioApertura) { this.usuarioApertura = usuarioApertura; }
    public String getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(String usuarioCierre) { this.usuarioCierre = usuarioCierre; }
    public BigDecimal getFondoInicial() { return fondoInicial; }
    public void setFondoInicial(BigDecimal fondoInicial) { this.fondoInicial = fondoInicial; }
    public BigDecimal getVentasEfectivo() { return ventasEfectivo; }
    public void setVentasEfectivo(BigDecimal ventasEfectivo) { this.ventasEfectivo = ventasEfectivo; }
    public BigDecimal getEntradas() { return entradas; }
    public void setEntradas(BigDecimal entradas) { this.entradas = entradas; }
    public BigDecimal getSalidas() { return salidas; }
    public void setSalidas(BigDecimal salidas) { this.salidas = salidas; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public List<MovimientoDTO> getMovimientos() { return movimientos; }
    public void setMovimientos(List<MovimientoDTO> movimientos) { this.movimientos = movimientos; }
}
