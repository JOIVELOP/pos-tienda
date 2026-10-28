package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.DetalleVentaDTO;
import com.pos.pos.tienda.dto.ReporteDTO;
import com.pos.pos.tienda.dto.VentaDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class VentaServiceImpl implements VentaService {

    private static final String VENTA_NO_ENCONTRADA = "Venta no encontrada: ";
    private static final Logger log = LoggerFactory.getLogger(VentaServiceImpl.class);

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PromocionRepository promocionRepository;
    private final TurnoCajaRepository turnoCajaRepository;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            ProductoRepository productoRepository,
                            ClienteRepository clienteRepository,
                            UsuarioRepository usuarioRepository,
                            PromocionRepository promocionRepository,
                            TurnoCajaRepository turnoCajaRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.promocionRepository = promocionRepository;
        this.turnoCajaRepository = turnoCajaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaDTO> findAll() {
        return ventaRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaDTO findById(Long id) {
        return toDTO(ventaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public VentaDTO findByFolio(String folio) {
        return toDTO(ventaRepository.findByFolio(folio)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + folio)));
    }

    @Override
    @Transactional
    public VentaDTO save(VentaDTO dto, String username) {
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new BusinessException("La venta debe incluir al menos un producto");
        }
        BigDecimal descuento = dto.getDescuento() != null ? dto.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El descuento no puede ser negativo");
        }

        Venta venta = new Venta();
        venta.setFolio("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        venta.setFecha(LocalDateTime.now());
        venta.setMetodoPago(dto.getMetodoPago());
        venta.setEstatus(Venta.Estatus.COMPLETADA);
        venta.setDescuento(descuento);

        // Client-provided prices and totals are not trusted; amounts are recalculated from products.
        if (dto.getIdCliente() != null) {
            clienteRepository.findById(dto.getIdCliente()).ifPresent(venta::setCliente);
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
        TurnoCaja turnoCaja = turnoCajaRepository.findOpenByUsernameForUpdate(
                        username, TurnoCaja.Estado.ABIERTO)
                .orElseThrow(() -> new BusinessException("Abre tu turno de caja antes de cobrar"));
        if (descuento.compareTo(BigDecimal.ZERO) > 0
                && usuario.getRol() != Usuario.Rol.ADMINISTRADOR
                && usuario.getRol() != Usuario.Rol.ENCARGADO) {
            throw new BusinessException("Se requiere autorización de encargado para aplicar descuentos");
        }
        venta.setUsuario(usuario);
        venta.setTurnoCaja(turnoCaja);

        Map<Long, Integer> cantidadesPorProducto = new TreeMap<>();
        for (DetalleVentaDTO detalle : dto.getDetalles()) {
            if (detalle.getIdProducto() == null || detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {
                throw new BusinessException("Producto o cantidad inválida");
            }
            try {
                cantidadesPorProducto.merge(detalle.getIdProducto(), detalle.getCantidad(),
                        Math::addExact);
            } catch (ArithmeticException ex) {
                throw new BusinessException("La cantidad solicitada es demasiado grande");
            }
        }

        Map<Long, Producto> productosBloqueados = new TreeMap<>();
        cantidadesPorProducto.forEach((idProducto, cantidad) -> {
            Producto producto = productoRepository.findByIdForUpdate(idProducto)
                    .orElseThrow(() -> new BusinessException("Producto no encontrado: " + idProducto));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new BusinessException("El producto está inactivo: " + producto.getNombre());
            }
            if (producto.getStock() < cantidad) {
                throw new BusinessException("Stock insuficiente para: " + producto.getNombre());
            }
            productosBloqueados.put(idProducto, producto);
        });

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entrada : cantidadesPorProducto.entrySet()) {
            Producto producto = productosBloqueados.get(entrada.getKey());
            int cantidad = entrada.getValue();

            List<Promocion> promociones = promocionRepository
                    .findPromocionVigenteByProducto(producto.getId(), LocalDate.now());

            BigDecimal precioFinal = producto.getPrecioVenta();
            BigDecimal descuentoItem = BigDecimal.ZERO;

            if (!promociones.isEmpty()) {
                Promocion promo = promociones.get(0);
                switch (promo.getTipo()) {
                    case PORCENTAJE:
                        if (promo.getValor().compareTo(BigDecimal.ZERO) < 0
                                || promo.getValor().compareTo(BigDecimal.valueOf(100)) > 0) {
                            throw new BusinessException("La promoción porcentual debe estar entre 0 y 100");
                        }
                        descuentoItem = precioFinal
                                .multiply(promo.getValor())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(cantidad));
                        break;
                    case MONTO_FIJO:
                        if (promo.getValor().compareTo(BigDecimal.ZERO) < 0) {
                            throw new BusinessException("El descuento de la promoción no puede ser negativo");
                        }
                        descuentoItem = promo.getValor().min(precioFinal)
                                .multiply(BigDecimal.valueOf(cantidad));
                        break;
                    case DOS_X_UNO:
                        long gratis = cantidad / 2;
                        descuentoItem = precioFinal.multiply(BigDecimal.valueOf(gratis));
                        break;
                }
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioFinal);
            detalle.setDescuento(descuentoItem);
            detalle.setSubtotal(precioFinal
                    .multiply(BigDecimal.valueOf(cantidad))
                    .subtract(descuentoItem));
            detalle.setVenta(venta);

            subtotal = subtotal.add(detalle.getSubtotal());
            detalles.add(detalle);
        }

        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El subtotal calculado debe ser mayor a 0");
        }
        if (descuento.compareTo(subtotal) >= 0) {
            throw new BusinessException("El descuento debe ser menor que el subtotal");
        }

        cantidadesPorProducto.forEach((idProducto, cantidad) -> {
            Producto producto = productosBloqueados.get(idProducto);
            producto.setStock(producto.getStock() - cantidad);
            productoRepository.save(producto);
        });

        venta.setSubtotal(subtotal);
        venta.setTotal(subtotal.subtract(venta.getDescuento()));
        venta.setDetalles(detalles);

        ventaRepository.saveAndFlush(venta);
        venta.setFolio(String.format("VTA-%010d", venta.getId()));
        Venta ventaGuardada = ventaRepository.save(venta);
        log.info("Venta registrada: folio={}, usuario={}, total={}",
                ventaGuardada.getFolio(), username, ventaGuardada.getTotal());
        return toDTO(ventaGuardada);
    }

    @Override
    @Transactional
    public VentaDTO cancelar(Long id) {
        Venta venta = ventaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + id));
        if (venta.getEstatus() == Venta.Estatus.CANCELADA) {
            log.info("Solicitud de cancelación repetida: folio={}", venta.getFolio());
            return toDTO(venta);
        }
        if (venta.getEstatus() != Venta.Estatus.COMPLETADA) {
            throw new BusinessException("Solo se pueden cancelar ventas completadas");
        }
        Map<Long, Integer> cantidadesPorProducto = new TreeMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            cantidadesPorProducto.merge(detalle.getProducto().getId(), detalle.getCantidad(),
                    Math::addExact);
        }
        cantidadesPorProducto.forEach((idProducto, cantidad) -> {
            Producto producto = productoRepository.findByIdForUpdate(idProducto)
                    .orElseThrow(() -> new BusinessException("Producto no encontrado: " + idProducto));
            try {
                producto.setStock(Math.addExact(producto.getStock(), cantidad));
            } catch (ArithmeticException ex) {
                throw new BusinessException("La existencia resultante excede el límite permitido");
            }
            productoRepository.save(producto);
        });
        venta.setEstatus(Venta.Estatus.CANCELADA);

        Venta ventaCancelada = ventaRepository.save(venta);
        log.info("Venta cancelada: folio={}", ventaCancelada.getFolio());
        return toDTO(ventaCancelada);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO generarReporte(LocalDateTime inicio, LocalDateTime fin) {
        ReporteDTO reporte = new ReporteDTO();
        BigDecimal total = ventaRepository.totalVentasPorPeriodo(inicio, fin);
        List<Venta> ventas = ventaRepository.findByFechaBetweenAndEstatus(
                inicio, fin, Venta.Estatus.COMPLETADA);

        reporte.setTotalVentas(total);
        reporte.setCantidadVentas((long) ventas.size());
        reporte.setPromedioVenta(ventas.isEmpty() ? BigDecimal.ZERO :
                total.divide(BigDecimal.valueOf(ventas.size()), 2, RoundingMode.HALF_UP));

        // Productos más vendidos
        List<ReporteDTO.ProductoVendidoDTO> productosVendidos = detalleVentaRepository
                .productosMasVendidos().stream().map(row -> {
                    ReporteDTO.ProductoVendidoDTO p = new ReporteDTO.ProductoVendidoDTO();
                    p.setNombreProducto(row[0] != null ? row[0].toString() : "");
                    p.setCantidadVendida(((Number) row[1]).longValue());
                    p.setTotalGenerado((BigDecimal) row[2]);
                    return p;
                }).toList();
        reporte.setProductosMasVendidos(productosVendidos);

        // Ventas por día
        List<ReporteDTO.VentaDiariaDTO> ventasPorDia = ventaRepository
                .reporteVentasPorDia(inicio, fin).stream().map(row -> {
                    ReporteDTO.VentaDiariaDTO v = new ReporteDTO.VentaDiariaDTO();
                    Object fecha = row[0];
                    if (fecha instanceof java.sql.Date) {
                        v.setFecha(fecha.toString());
                    } else if (fecha instanceof java.time.LocalDate) {
                        v.setFecha(fecha.toString());
                    } else if (fecha instanceof Long) {
                        v.setFecha(new java.sql.Date((Long) fecha).toString());
                    } else {
                        v.setFecha(String.valueOf(fecha));
                    }
                    v.setCantidadVentas(((Number) row[1]).longValue());
                    v.setTotalVentas((BigDecimal) row[2]);
                    return v;
                }).toList();
        reporte.setVentasPorDia(ventasPorDia);

        // Ventas por hora ← DENTRO del método, no afuera
        List<ReporteDTO.VentaHorariaDTO> ventasPorHora = ventaRepository
                .reporteVentasPorHora(inicio, fin).stream().map(row -> {
                    ReporteDTO.VentaHorariaDTO h = new ReporteDTO.VentaHorariaDTO();
                    h.setHora(((Number) row[0]).intValue());
                    h.setCantidadVentas(((Number) row[1]).longValue());
                    h.setTotalVentas((BigDecimal) row[2]);
                    return h;
                }).toList();
        reporte.setVentasPorHora(ventasPorHora);

        return reporte;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteDTO generarReporteDetallado(LocalDateTime inicio, LocalDateTime fin) {
        return generarReporte(inicio, fin);
    }

    private VentaDTO toDTO(Venta v) {
        VentaDTO dto = new VentaDTO();
        dto.setId(v.getId());
        dto.setFolio(v.getFolio());
        dto.setFecha(v.getFecha());
        dto.setSubtotal(v.getSubtotal());
        dto.setDescuento(v.getDescuento());
        dto.setTotal(v.getTotal());
        dto.setMetodoPago(v.getMetodoPago());
        dto.setEstatus(v.getEstatus());
        if (v.getCliente() != null) {
            dto.setIdCliente(v.getCliente().getId());
            dto.setNombreCliente(v.getCliente().getNombre());
        }
        if (v.getUsuario() != null) {
            dto.setIdUsuario(v.getUsuario().getId());
            dto.setNombreUsuario(v.getUsuario().getNombre());
        }
        if (v.getDetalles() != null) {
            dto.setDetalles(v.getDetalles().stream().map(d -> {
                DetalleVentaDTO dd = new DetalleVentaDTO();
                dd.setId(d.getId());
                dd.setCantidad(d.getCantidad());
                dd.setPrecioUnitario(d.getPrecioUnitario());
                dd.setDescuento(d.getDescuento());
                dd.setSubtotal(d.getSubtotal());
                dd.setIdProducto(d.getProducto().getId());
                dd.setNombreProducto(d.getProducto().getNombre());
                dd.setCodigoProducto(d.getProducto().getCodigo());
                return dd;
            }).toList());
        }
        return dto;
    }
}