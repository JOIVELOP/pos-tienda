package com.pos.pos.tienda.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "T_DEVOLUCION")
public class DevolucionVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_reembolso", nullable = false, length = 20)
    private Venta.MetodoPago metodoReembolso;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalReembolsado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_turno_caja")
    private TurnoCaja turnoCaja;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaDevolucion> lineas = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Venta.MetodoPago getMetodoReembolso() { return metodoReembolso; }
    public void setMetodoReembolso(Venta.MetodoPago metodoReembolso) { this.metodoReembolso = metodoReembolso; }
    public BigDecimal getTotalReembolsado() { return totalReembolsado; }
    public void setTotalReembolsado(BigDecimal totalReembolsado) { this.totalReembolsado = totalReembolsado; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public TurnoCaja getTurnoCaja() { return turnoCaja; }
    public void setTurnoCaja(TurnoCaja turnoCaja) { this.turnoCaja = turnoCaja; }
    public List<LineaDevolucion> getLineas() { return lineas; }
    public void setLineas(List<LineaDevolucion> lineas) { this.lineas = lineas; }
}
