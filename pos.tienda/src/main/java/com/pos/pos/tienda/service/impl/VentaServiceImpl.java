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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaServiceImpl implements VentaService {

    private static final String VENTA_NO_ENCONTRADA = "Venta no encontrada: ";

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PromocionRepository promocionRepository;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            ProductoRepository productoRepository,
                            ClienteRepository clienteRepository,
                            UsuarioRepository usuarioRepository,
                            PromocionRepository promocionRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.promocionRepository = promocionRepository;
    }

    @Override
    public List<VentaDTO> findAll() {
        return ventaRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public VentaDTO findById(Long id) {
        return toDTO(ventaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + id)));
    }

    @Override
    public VentaDTO findByFolio(String folio) {
        return toDTO(ventaRepository.findByFolio(folio)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + folio)));
    }

    @Override
    @Transactional
    public VentaDTO save(VentaDTO dto) {
        Venta venta = new Venta();
        venta.setFolio(generarFolio());
        venta.setFecha(LocalDateTime.now());
        venta.setMetodoPago(dto.getMetodoPago());
        venta.setEstatus(Venta.Estatus.COMPLETADA);
        venta.setDescuento(dto.getDescuento() != null ? dto.getDescuento() : BigDecimal.ZERO);

        if (dto.getIdCliente() != null) {
            clienteRepository.findById(dto.getIdCliente()).ifPresent(venta::setCliente);
        }

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        venta.setUsuario(usuario);

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (DetalleVentaDTO detalleDTO : dto.getDetalles()) {
            if (detalleDTO.getCantidad() <= 0) {
                throw new BusinessException("Cantidad inválida");
            }

            Producto producto = productoRepository.findById(detalleDTO.getIdProducto())
                    .orElseThrow(() -> new BusinessException("Producto no encontrado"));

            if (producto.getStock() < detalleDTO.getCantidad()) {
                throw new BusinessException("Stock insuficiente para: " + producto.getNombre());
            }

            List<Promocion> promociones = promocionRepository
                    .findPromocionVigenteByProducto(producto.getId(), LocalDate.now());

            BigDecimal precioFinal = producto.getPrecioVenta();
            BigDecimal descuentoItem = BigDecimal.ZERO;

            if (!promociones.isEmpty()) {
                Promocion promo = promociones.get(0);
                switch (promo.getTipo()) {
                    case PORCENTAJE:
                        descuentoItem = precioFinal
                                .multiply(promo.getValor())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(detalleDTO.getCantidad()));
                        break;
                    case MONTO_FIJO:
                        descuentoItem = promo.getValor()
                                .multiply(BigDecimal.valueOf(detalleDTO.getCantidad()));
                        break;
                    case DOS_X_UNO:
                        long gratis = detalleDTO.getCantidad() / 2;
                        descuentoItem = precioFinal.multiply(BigDecimal.valueOf(gratis));
                        break;
                }
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(precioFinal);
            detalle.setDescuento(descuentoItem);
            detalle.setSubtotal(precioFinal
                    .multiply(BigDecimal.valueOf(detalleDTO.getCantidad()))
                    .subtract(descuentoItem));
            detalle.setVenta(venta);

            subtotal = subtotal.add(detalle.getSubtotal());
            producto.setStock(producto.getStock() - detalleDTO.getCantidad());
            productoRepository.save(producto);
            detalles.add(detalle);
        }

        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El total calculado debe ser mayor a 0");
        }

        venta.setSubtotal(subtotal);
        venta.setTotal(subtotal.subtract(venta.getDescuento()));
        venta.setDetalles(detalles);

        return toDTO(ventaRepository.save(venta));
    }

    @Override
    @Transactional
    public VentaDTO cancelar(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(VENTA_NO_ENCONTRADA + id));
        venta.setEstatus(Venta.Estatus.CANCELADA);

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
        }

        return toDTO(ventaRepository.save(venta));
    }

    @Override
    public ReporteDTO generarReporte(LocalDateTime inicio, LocalDateTime fin) {
        ReporteDTO reporte = new ReporteDTO();
        BigDecimal total = ventaRepository.totalVentasPorPeriodo(inicio, fin);
        List<Venta> ventas = ventaRepository.findByFechaBetween(inicio, fin);

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
    public ReporteDTO generarReporteDetallado(LocalDateTime inicio, LocalDateTime fin) {
        return generarReporte(inicio, fin);
    }

    private String generarFolio() {
        String ultimo = ventaRepository.findUltimoFolio().orElse("VTA-0000");
        int numero = Integer.parseInt(ultimo.replace("VTA-", "")) + 1;
        return String.format("VTA-%04d", numero);
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