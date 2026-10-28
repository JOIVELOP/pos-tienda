package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.DevolucionDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.DevolucionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DevolucionServiceImpl implements DevolucionService {
    private static final Logger log = LoggerFactory.getLogger(DevolucionServiceImpl.class);
    private final VentaRepository ventaRepository;
    private final DevolucionVentaRepository devolucionRepository;
    private final LineaDevolucionRepository lineaDevolucionRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;

    public DevolucionServiceImpl(VentaRepository ventaRepository,
                                 DevolucionVentaRepository devolucionRepository,
                                 LineaDevolucionRepository lineaDevolucionRepository,
                                 ProductoRepository productoRepository,
                                 UsuarioRepository usuarioRepository,
                                 TurnoCajaRepository turnoCajaRepository,
                                 MovimientoCajaRepository movimientoCajaRepository) {
        this.ventaRepository = ventaRepository;
        this.devolucionRepository = devolucionRepository;
        this.lineaDevolucionRepository = lineaDevolucionRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.turnoCajaRepository = turnoCajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
    }

    @Override
    @Transactional
    public DevolucionDTO crear(DevolucionDTO.CrearRequest request, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
        if (usuario.getRol() != Usuario.Rol.ADMINISTRADOR
                && usuario.getRol() != Usuario.Rol.ENCARGADO) {
            throw new BusinessException("Se requiere autorización de encargado para registrar devoluciones");
        }
        TurnoCaja turno = turnoCajaRepository.findOpenByUsernameForUpdate(
                        username, TurnoCaja.Estado.ABIERTO)
                .orElseThrow(() -> new BusinessException("Abre tu turno de caja antes de registrar una devolución"));
        Venta venta = ventaRepository.findByIdForUpdate(request.getIdVenta())
                .orElseThrow(() -> new BusinessException("Venta no encontrada: " + request.getIdVenta()));
        if (venta.getEstatus() != Venta.Estatus.COMPLETADA) {
            throw new BusinessException("Solo se pueden devolver artículos de ventas completadas");
        }

        Map<Long, DetalleVenta> detallesVenta = new HashMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            detallesVenta.put(detalle.getId(), detalle);
        }
        Set<Long> idsDetalle = new HashSet<>();
        Map<Long, Integer> cantidadesDevueltas = new HashMap<>();
        Map<Long, BigDecimal> importesDevueltos = new HashMap<>();
        for (DevolucionDTO.LineaRequest lineaRequest : request.getLineas()) {
            if (lineaRequest.getIdDetalleVenta() == null || lineaRequest.getCantidad() == null
                    || lineaRequest.getCantidad() <= 0 || lineaRequest.getReingresaInventario() == null
                    || !idsDetalle.add(lineaRequest.getIdDetalleVenta())) {
                throw new BusinessException("Los artículos a devolver deben ser válidos y no repetidos");
            }
            if (!detallesVenta.containsKey(lineaRequest.getIdDetalleVenta())) {
                throw new BusinessException("El artículo no pertenece a la venta indicada");
            }
            for (LineaDevolucion anterior : lineaDevolucionRepository
                    .findByDetalleVentaId(lineaRequest.getIdDetalleVenta())) {
                cantidadesDevueltas.merge(anterior.getDetalleVenta().getId(),
                        anterior.getCantidad(), Math::addExact);
                importesDevueltos.merge(anterior.getDetalleVenta().getId(),
                        anterior.getMontoReembolsado(), BigDecimal::add);
            }
        }

        Map<Long, Producto> productosReingresados = new TreeMap<>();
        Map<Long, Integer> cantidadesReingresar = new TreeMap<>();
        Map<Long, BigDecimal> montoPorDetalle = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DevolucionDTO.LineaRequest lineaRequest : request.getLineas()) {
            DetalleVenta detalle = detallesVenta.get(lineaRequest.getIdDetalleVenta());
            int devueltoAntes = cantidadesDevueltas.getOrDefault(detalle.getId(), 0);
            int cantidadRestante = detalle.getCantidad() - devueltoAntes;
            if (lineaRequest.getCantidad() > cantidadRestante) {
                throw new BusinessException("La cantidad excede lo que queda por devolver de "
                        + detalle.getProducto().getNombre());
            }
            BigDecimal importeRestante = detalle.getSubtotal().subtract(
                    importesDevueltos.getOrDefault(detalle.getId(), BigDecimal.ZERO));
            BigDecimal importe;
            if (lineaRequest.getCantidad() == cantidadRestante) {
                importe = importeRestante;
            } else {
                importe = importeRestante.multiply(BigDecimal.valueOf(lineaRequest.getCantidad()))
                        .divide(BigDecimal.valueOf(cantidadRestante), 2, RoundingMode.HALF_UP);
            }
            if (importe.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("El artículo no tiene saldo reembolsable");
            }
            montoPorDetalle.put(detalle.getId(), importe);
            total = total.add(importe);
            if (Boolean.TRUE.equals(lineaRequest.getReingresaInventario())) {
                cantidadesReingresar.merge(detalle.getProducto().getId(),
                        lineaRequest.getCantidad(), Math::addExact);
            }
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto de la devolución debe ser mayor a cero");
        }
        if (request.getMetodoReembolso() == Venta.MetodoPago.EFECTIVO) {
            BigDecimal cashSales = zeroIfNull(ventaRepository.sumCashSalesByTurnoId(turno.getId()));
            BigDecimal entries = zeroIfNull(movimientoCajaRepository.sumMontoByTurnoAndTipo(
                    turno, MovimientoCaja.Tipo.ENTRADA));
            BigDecimal exits = zeroIfNull(movimientoCajaRepository.sumMontoByTurnoAndTipo(
                    turno, MovimientoCaja.Tipo.SALIDA));
            BigDecimal cashAvailable = turno.getFondoInicial().add(cashSales).add(entries).subtract(exits);
            if (total.compareTo(cashAvailable) > 0) {
                throw new BusinessException("El efectivo disponible en caja es insuficiente para este reembolso");
            }
        }

        cantidadesReingresar.forEach((productId, quantity) -> {
            Producto producto = productoRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException("Producto no encontrado: " + productId));
            try {
                producto.setStock(Math.addExact(producto.getStock(), quantity));
            } catch (ArithmeticException ex) {
                throw new BusinessException("La existencia resultante excede el límite permitido");
            }
            productosReingresados.put(productId, producto);
        });

        DevolucionVenta devolucion = new DevolucionVenta();
        devolucion.setFolio("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        devolucion.setFecha(LocalDateTime.now());
        devolucion.setMotivo(request.getMotivo().trim());
        devolucion.setMetodoReembolso(request.getMetodoReembolso());
        devolucion.setTotalReembolsado(total);
        devolucion.setVenta(venta);
        devolucion.setUsuario(usuario);
        devolucion.setTurnoCaja(turno);
        for (DevolucionDTO.LineaRequest lineaRequest : request.getLineas()) {
            LineaDevolucion linea = new LineaDevolucion();
            linea.setDevolucion(devolucion);
            linea.setDetalleVenta(detallesVenta.get(lineaRequest.getIdDetalleVenta()));
            linea.setCantidad(lineaRequest.getCantidad());
            linea.setReingresaInventario(lineaRequest.getReingresaInventario());
            linea.setMontoReembolsado(montoPorDetalle.get(lineaRequest.getIdDetalleVenta()));
            devolucion.getLineas().add(linea);
        }

        productosReingresados.values().forEach(productoRepository::save);
        devolucionRepository.saveAndFlush(devolucion);
        devolucion.setFolio(String.format("DEV-%010d", devolucion.getId()));
        devolucionRepository.save(devolucion);

        if (request.getMetodoReembolso() == Venta.MetodoPago.EFECTIVO) {
            MovimientoCaja movimiento = new MovimientoCaja();
            movimiento.setTipo(MovimientoCaja.Tipo.SALIDA);
            movimiento.setConcepto("DEVOLUCION");
            movimiento.setDescripcion("Reembolso " + devolucion.getFolio()
                    + " de venta " + venta.getFolio());
            movimiento.setMonto(total);
            movimiento.setFecha(LocalDateTime.now());
            movimiento.setTurno(turno);
            movimiento.setUsuario(usuario);
            movimientoCajaRepository.save(movimiento);
        }
        log.info("Devolución registrada: folio={}, venta={}, usuario={}, total={}, método={}",
                devolucion.getFolio(), venta.getFolio(), username, total,
                devolucion.getMetodoReembolso());
        return toDTO(devolucion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionDTO> findByVenta(Long idVenta) {
        return devolucionRepository.findByVentaIdOrderByFechaAsc(idVenta)
                .stream().map(this::toDTO).toList();
    }

    private DevolucionDTO toDTO(DevolucionVenta devolucion) {
        DevolucionDTO dto = new DevolucionDTO();
        dto.setId(devolucion.getId());
        dto.setFolio(devolucion.getFolio());
        dto.setFecha(devolucion.getFecha());
        dto.setFolioVenta(devolucion.getVenta().getFolio());
        dto.setMotivo(devolucion.getMotivo());
        dto.setMetodoReembolso(devolucion.getMetodoReembolso());
        dto.setTotalReembolsado(devolucion.getTotalReembolsado());
        dto.setUsuario(devolucion.getUsuario().getUsername());
        dto.setLineas(devolucion.getLineas().stream().map(linea -> {
            DevolucionDTO.LineaDTO lineaDTO = new DevolucionDTO.LineaDTO();
            lineaDTO.setIdDetalleVenta(linea.getDetalleVenta().getId());
            lineaDTO.setProducto(linea.getDetalleVenta().getProducto().getNombre());
            lineaDTO.setCantidad(linea.getCantidad());
            lineaDTO.setReingresaInventario(linea.getReingresaInventario());
            lineaDTO.setMontoReembolsado(linea.getMontoReembolsado());
            return lineaDTO;
        }).toList());
        return dto;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
