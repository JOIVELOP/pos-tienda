package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.OfflineDTO;
import com.pos.pos.tienda.dto.TurnoCajaDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.TurnoCajaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OfflineService {
    private static final Logger log = LoggerFactory.getLogger(OfflineService.class);
    private final SesionOfflineRepository sesiones;
    private final OperacionOfflineRepository operaciones;
    private final UsuarioRepository usuarios;
    private final TurnoCajaRepository turnos;
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final MovimientoCajaRepository movimientos;
    private final TurnoCajaService caja;

    public OfflineService(SesionOfflineRepository sesiones, OperacionOfflineRepository operaciones,
                          UsuarioRepository usuarios, TurnoCajaRepository turnos,
                          ProductoRepository productos, VentaRepository ventas,
                          MovimientoCajaRepository movimientos, TurnoCajaService caja) {
        this.sesiones = sesiones;
        this.operaciones = operaciones;
        this.usuarios = usuarios;
        this.turnos = turnos;
        this.productos = productos;
        this.ventas = ventas;
        this.movimientos = movimientos;
        this.caja = caja;
    }

    @Transactional
    public void exigirSinSesion(String username) {
        usuarioActivo(username);
        if (sesiones.existsByTurnoUsuarioAperturaUsernameAndCerradaFalse(username)) {
            throw new BusinessException("Usa el diario offline de la caja preparada y sincroniza su cierre");
        }
    }

    @Transactional
    public void exigirVentaEditable(Long idVenta) {
        Venta venta = ventas.findById(idVenta)
                .orElseThrow(() -> new BusinessException("Venta no encontrada"));
        usuarioActivo(venta.getUsuario().getUsername());
        if (venta.getTurnoCaja() != null
                && sesiones.findByTurnoId(venta.getTurnoCaja().getId()).isPresent()) {
            throw new BusinessException("Una venta de caja offline requiere devolución, no cancelación");
        }
    }

    @Transactional
    public OfflineDTO.Sesion preparar(OfflineDTO.Preparar request, String username) {
        Usuario usuario = usuarioActivo(username);
        TurnoCaja turno = turnos.findOpenByUsernameForUpdate(username, TurnoCaja.Estado.ABIERTO)
                .orElseThrow(() -> new BusinessException("Abre tu turno online antes de preparar el modo offline"));
        SesionOffline sesion = sesiones.findByTurnoId(turno.getId()).orElse(null);
        if (sesion == null) {
            sesion = new SesionOffline();
            sesion.setId(UUID.randomUUID().toString());
            sesion.setDispositivo(request.dispositivo());
            sesion.setTurno(turno);
            sesion.setVence(Instant.now().plus(24, ChronoUnit.HOURS));
            sesion.setEfectivo(caja.turnoActual(username).orElseThrow().getEfectivoEsperado());
            for (Producto producto : productos.findByActivoTrue()) {
                if (producto.getPrecioVenta().signum() <= 0) continue;
                SesionOffline.Articulo articulo = new SesionOffline.Articulo();
                articulo.setId(producto.getId());
                articulo.setCodigo(producto.getCodigo());
                articulo.setNombre(producto.getNombre());
                articulo.setPrecioVenta(producto.getPrecioVenta());
                articulo.setStock(producto.getStock());
                sesion.getProductos().add(articulo);
            }
            sesiones.saveAndFlush(sesion);
            log.info("Caja preparada offline: turno={}, usuario={}, dispositivo={}",
                    turno.getId(), username, request.dispositivo());
        }
        if (!sesion.getDispositivo().equals(request.dispositivo())) {
            throw new BusinessException("Este turno ya está preparado en otro navegador o equipo");
        }
        return new OfflineDTO.Sesion(sesion.getId(), sesion.getDispositivo(), usuario.getId(),
                turno.getId(), turno.getFolio(), sesion.getVence(), sesion.isCerrada(),
                sesion.getSecuencia(), sesion.getEfectivo(), sesion.getProductos());
    }

    @Transactional
    public OfflineDTO.Confirmacion sincronizar(OfflineDTO.Operacion request, String username) {
        usuarioActivo(username); // Serializes preparation and synchronization for this employee.
        SesionOffline sesion = sesiones.findById(request.sesion())
                .orElseThrow(() -> new BusinessException("Preparación offline no encontrada"));
        if (!sesion.getTurno().getUsuarioApertura().getUsername().equals(username)
                || !sesion.getDispositivo().equals(request.dispositivo())) {
            throw new BusinessException("La operación pertenece a otro usuario o dispositivo");
        }
        String huella = huella(request);
        OperacionOffline anterior = operaciones.findById(request.id()).orElse(null);
        if (anterior != null) {
            if (!anterior.getSesion().getId().equals(sesion.getId())
                    || !anterior.getHuella().equals(huella)) {
                throw new BusinessException("Identificador reutilizado con contenido distinto");
            }
            return new OfflineDTO.Confirmacion(anterior.getId(), anterior.getFolio(), anterior.getSecuencia());
        }
        TurnoCaja turno = turnos.findByIdForUpdate(sesion.getTurno().getId()).orElseThrow();
        if (sesion.isCerrada() || turno.getEstado() != TurnoCaja.Estado.ABIERTO) {
            throw new BusinessException("El turno ya está cerrado; requiere conciliación");
        }
        if (request.secuencia() != sesion.getSecuencia() + 1) {
            throw new BusinessException("Sincroniza primero las operaciones anteriores del turno");
        }
        if (request.captura().isBefore(turno.getApertura().atZone(java.time.ZoneId.systemDefault()).toInstant())
                || request.captura().isAfter(Instant.now().plusSeconds(300))
                || (request.tipo() != OfflineDTO.Tipo.CIERRE && request.captura().isAfter(sesion.getVence()))) {
            throw new BusinessException("Fecha fuera de la autorización offline; requiere conciliación");
        }
        String folio;
        if (request.tipo() == OfflineDTO.Tipo.VENTA) {
            folio = registrarVenta(request, sesion, turno);
        } else {
            if (!request.lineas().isEmpty()) throw new BusinessException("Este movimiento no admite artículos");
            if (request.tipo() == OfflineDTO.Tipo.CIERRE) {
                TurnoCajaDTO.CierreRequest cierre = new TurnoCajaDTO.CierreRequest();
                cierre.setEfectivoContado(request.monto());
                caja.cerrar(turno.getId(), cierre, username);
                sesion.setCerrada(true);
                folio = turno.getFolio();
            } else {
                if (request.monto().signum() <= 0) throw new BusinessException("El monto debe ser positivo");
                boolean entrada = request.tipo() == OfflineDTO.Tipo.APORTACION;
                if (!entrada && request.monto().compareTo(sesion.getEfectivo()) > 0) {
                    throw new BusinessException("Salida mayor al efectivo disponible");
                }
                MovimientoCaja movimiento = new MovimientoCaja();
                movimiento.setTurno(turno);
                movimiento.setUsuario(turno.getUsuarioApertura());
                movimiento.setTipo(entrada ? MovimientoCaja.Tipo.ENTRADA : MovimientoCaja.Tipo.SALIDA);
                movimiento.setConcepto(request.tipo().name());
                movimiento.setDescripcion(request.descripcion());
                movimiento.setMonto(request.monto());
                movimiento.setFecha(java.time.LocalDateTime.ofInstant(
                        request.captura(), java.time.ZoneId.systemDefault()));
                movimientos.saveAndFlush(movimiento);
                sesion.setEfectivo(entrada ? sesion.getEfectivo().add(request.monto())
                        : sesion.getEfectivo().subtract(request.monto()));
                folio = "MOV-" + movimiento.getId();
            }
        }
        sesion.setSecuencia(request.secuencia());
        OperacionOffline operacion = new OperacionOffline();
        operacion.setId(request.id());
        operacion.setSesion(sesion);
        operacion.setSecuencia(request.secuencia());
        operacion.setHuella(huella);
        operacion.setCaptura(request.captura());
        operacion.setSincronizacion(Instant.now());
        operacion.setFolio(folio);
        operaciones.saveAndFlush(operacion);
        log.info("Operación offline confirmada: id={}, turno={}, secuencia={}, folio={}",
                request.id(), turno.getId(), request.secuencia(), folio);
        return new OfflineDTO.Confirmacion(request.id(), folio, request.secuencia());
    }

    private String registrarVenta(OfflineDTO.Operacion request, SesionOffline sesion, TurnoCaja turno) {
        if (request.lineas().isEmpty()) throw new BusinessException("Venta sin artículos");
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (OfflineDTO.Linea linea : request.lineas()) {
            if (linea.cantidad() <= 0 || cantidades.putIfAbsent(linea.idProducto(), linea.cantidad()) != null) {
                throw new BusinessException("Artículos inválidos o repetidos");
            }
        }
        List<DetalleVenta> detalles = new ArrayList<>();
        Venta venta = new Venta();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            SesionOffline.Articulo autorizado = sesion.getProductos().stream()
                    .filter(p -> p.getId().equals(entry.getKey())).findFirst()
                    .orElseThrow(() -> new BusinessException("Producto no autorizado en el catálogo offline"));
            Producto producto = productos.findByIdForUpdate(entry.getKey()).orElseThrow();
            if (entry.getValue() > autorizado.getStock() || entry.getValue() > producto.getStock()) {
                throw new BusinessException("Conflicto de stock en " + autorizado.getNombre()
                        + ". Conserva el pendiente y solicita conciliación; no vuelvas a cobrar.");
            }
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(entry.getValue());
            detalle.setPrecioUnitario(autorizado.getPrecioVenta());
            detalle.setSubtotal(autorizado.getPrecioVenta().multiply(BigDecimal.valueOf(entry.getValue())));
            total = total.add(detalle.getSubtotal());
            detalles.add(detalle);
            producto.setStock(producto.getStock() - entry.getValue());
            autorizado.setStock(autorizado.getStock() - entry.getValue());
        }
        if (total.compareTo(request.monto()) != 0 || total.signum() <= 0) {
            throw new BusinessException("El total no coincide con los precios autorizados");
        }
        venta.setFolio("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        venta.setUsuario(turno.getUsuarioApertura());
        venta.setTurnoCaja(turno);
        venta.setFecha(java.time.LocalDateTime.ofInstant(request.captura(), java.time.ZoneId.systemDefault()));
        venta.setMetodoPago(Venta.MetodoPago.EFECTIVO);
        venta.setSubtotal(total);
        venta.setTotal(total);
        venta.setDetalles(detalles);
        ventas.saveAndFlush(venta);
        venta.setFolio(String.format("VTA-%010d", venta.getId()));
        sesion.setEfectivo(sesion.getEfectivo().add(total));
        return venta.getFolio();
    }

    private Usuario usuarioActivo(String username) {
        return usuarios.findByUsernameForUpdate(username).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
    }

    static String huella(OfflineDTO.Operacion request) {
        StringBuilder contenido = new StringBuilder().append(request.sesion()).append('|')
                .append(request.dispositivo()).append('|').append(request.secuencia()).append('|')
                .append(request.captura()).append('|').append(request.tipo()).append('|')
                .append(request.monto().stripTrailingZeros().toPlainString()).append('|')
                .append(request.descripcion().length()).append(':').append(request.descripcion());
        request.lineas().stream().sorted(Comparator.comparing(OfflineDTO.Linea::idProducto))
                .forEach(l -> contenido.append('|').append(l.idProducto()).append(':').append(l.cantidad()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contenido.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
