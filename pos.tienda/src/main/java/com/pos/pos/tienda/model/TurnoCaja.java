package com.pos.pos.tienda.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "T_TURNO_CAJA")
public class TurnoCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @Column(nullable = false)
    private LocalDateTime apertura = LocalDateTime.now();

    private LocalDateTime cierre;

    @Column(name = "fondo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal fondoInicial;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado = Estado.ABIERTO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_apertura", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre")
    private Usuario usuarioCierre;

    public enum Estado { ABIERTO, CERRADO }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public LocalDateTime getApertura() { return apertura; }
    public void setApertura(LocalDateTime apertura) { this.apertura = apertura; }
    public LocalDateTime getCierre() { return cierre; }
    public void setCierre(LocalDateTime cierre) { this.cierre = cierre; }
    public BigDecimal getFondoInicial() { return fondoInicial; }
    public void setFondoInicial(BigDecimal fondoInicial) { this.fondoInicial = fondoInicial; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(BigDecimal efectivoContado) { this.efectivoContado = efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Usuario getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(Usuario usuarioApertura) { this.usuarioApertura = usuarioApertura; }
    public Usuario getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(Usuario usuarioCierre) { this.usuarioCierre = usuarioCierre; }
}
