package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.CompraDTO;
import com.pos.pos.tienda.model.*;
import com.pos.pos.tienda.repository.*;
import com.pos.pos.tienda.service.CompraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CompraServiceImpl implements CompraService {
    private static final Logger log = LoggerFactory.getLogger(CompraServiceImpl.class);
    private static final String NOT_FOUND = "Compra no encontrada: ";

    private final CompraRepository compraRepository;
    private final RecepcionCompraRepository recepcionRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    public CompraServiceImpl(CompraRepository compraRepository,
                             RecepcionCompraRepository recepcionRepository,
                             ProductoRepository productoRepository,
                             ProveedorRepository proveedorRepository,
                             UsuarioRepository usuarioRepository) {
        this.compraRepository = compraRepository;
        this.recepcionRepository = recepcionRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDTO> findAll() {
        return compraRepository.findAllByOrderByFechaDesc().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompraDTO findById(Long id) {
        return toDTO(compraRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND + id)));
    }

    @Override
    @Transactional
    public CompraDTO crear(CompraDTO.CrearRequest request, String username) {
        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new BusinessException("Proveedor activo no encontrado"));
        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));
        Map<Long, CompraDTO.LineaRequest> requests = new TreeMap<>();
        for (CompraDTO.LineaRequest linea : request.getLineas()) {
            if (linea.getIdProducto() == null || linea.getCantidadSolicitada() == null
                    || linea.getCantidadSolicitada() <= 0 || linea.getCostoUnitario() == null
                    || linea.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0
                    || requests.putIfAbsent(linea.getIdProducto(), linea) != null) {
                throw new BusinessException("Las líneas de compra deben ser válidas y sin productos repetidos");
            }
        }

        Compra compra = new Compra();
        compra.setFolio("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        for (CompraDTO.LineaRequest lineaRequest : requests.values()) {
            Producto producto = productoRepository.findById(lineaRequest.getIdProducto())
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .orElseThrow(() -> new BusinessException(
                            "Producto activo no encontrado: " + lineaRequest.getIdProducto()));
            LineaCompra linea = new LineaCompra();
            linea.setCompra(compra);
            linea.setProducto(producto);
            linea.setCantidadSolicitada(lineaRequest.getCantidadSolicitada());
            linea.setCantidadRecibida(0);
            linea.setCostoUnitario(lineaRequest.getCostoUnitario());
            compra.getLineas().add(linea);
        }
        compraRepository.saveAndFlush(compra);
        compra.setFolio(String.format("COM-%010d", compra.getId()));
        Compra saved = compraRepository.save(compra);
        log.info("Orden de compra creada: folio={}, proveedor={}, usuario={}",
                saved.getFolio(), proveedor.getNombre(), username);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CompraDTO recibir(Long id, CompraDTO.RecepcionRequest request, String username) {
        Compra compra = compraRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND + id));
        if (compra.getEstado() == Compra.Estado.RECIBIDA) {
            throw new BusinessException("La compra ya fue recibida por completo");
        }
        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BusinessException("Usuario activo no encontrado"));

        Map<Long, LineaCompra> lineasCompra = new HashMap<>();
        for (LineaCompra linea : compra.getLineas()) {
            lineasCompra.put(linea.getId(), linea);
        }
        Map<Long, CompraDTO.LineaRecepcionRequest> received = new TreeMap<>();
        for (CompraDTO.LineaRecepcionRequest linea : request.getLineas()) {
            if (linea.getIdLineaCompra() == null || linea.getCantidadBuena() == null
                    || linea.getCantidadDanada() == null || linea.getCostoUnitario() == null
                    || linea.getCantidadBuena() < 0 || linea.getCantidadDanada() < 0
                    || linea.getCantidadBuena() + (long) linea.getCantidadDanada() <= 0
                    || linea.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0
                    || received.putIfAbsent(linea.getIdLineaCompra(), linea) != null) {
                throw new BusinessException("Las líneas recibidas deben ser válidas, únicas y tener cantidad");
            }
        }

        Map<Long, Integer> stockIngresado = new TreeMap<>();
        for (Map.Entry<Long, CompraDTO.LineaRecepcionRequest> entry : received.entrySet()) {
            LineaCompra lineaCompra = lineasCompra.get(entry.getKey());
            if (lineaCompra == null) {
                throw new BusinessException("La línea recibida no pertenece a esta compra");
            }
            CompraDTO.LineaRecepcionRequest detalle = entry.getValue();
            long totalRecibido = detalle.getCantidadBuena() + (long) detalle.getCantidadDanada();
            if (lineaCompra.getCantidadRecibida() + (long) totalRecibido
                    > lineaCompra.getCantidadSolicitada()) {
                throw new BusinessException("La recepción excede la cantidad solicitada de "
                        + lineaCompra.getProducto().getNombre());
            }
            lineaCompra.setCantidadRecibida(Math.toIntExact(
                    lineaCompra.getCantidadRecibida() + totalRecibido));
            if (detalle.getCantidadBuena() > 0) {
                try {
                    stockIngresado.merge(lineaCompra.getProducto().getId(),
                            detalle.getCantidadBuena(), Math::addExact);
                } catch (ArithmeticException ex) {
                    throw new BusinessException("La cantidad recibida excede el límite permitido");
                }
            }
        }

        Map<Long, BigDecimal> costosRecibidos = new TreeMap<>();
        received.forEach((lineId, lineRequest) -> {
            LineaCompra linea = lineasCompra.get(lineId);
            costosRecibidos.put(linea.getProducto().getId(), lineRequest.getCostoUnitario());
        });
        Set<Long> productosAfectados = new TreeSet<>(costosRecibidos.keySet());
        Map<Long, Producto> productos = new TreeMap<>();
        productosAfectados.forEach(productId -> {
            Producto producto = productoRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException("Producto no encontrado: " + productId));
            Integer quantity = stockIngresado.get(productId);
            if (quantity != null) {
                try {
                    producto.setStock(Math.addExact(producto.getStock(), quantity));
                } catch (ArithmeticException ex) {
                    throw new BusinessException("La existencia resultante excede el límite permitido");
                }
            }
            producto.setPrecioCompra(costosRecibidos.get(productId));
            productos.put(productId, producto);
        });

        RecepcionCompra recepcion = new RecepcionCompra();
        recepcion.setFolio("TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        recepcion.setCompra(compra);
        recepcion.setUsuario(usuario);
        recepcion.setFecha(LocalDateTime.now());
        for (Map.Entry<Long, CompraDTO.LineaRecepcionRequest> entry : received.entrySet()) {
            LineaCompra lineaCompra = lineasCompra.get(entry.getKey());
            CompraDTO.LineaRecepcionRequest requestLine = entry.getValue();
            LineaRecepcion lineaRecepcion = new LineaRecepcion();
            lineaRecepcion.setRecepcion(recepcion);
            lineaRecepcion.setLineaCompra(lineaCompra);
            lineaRecepcion.setCantidadBuena(requestLine.getCantidadBuena());
            lineaRecepcion.setCantidadDanada(requestLine.getCantidadDanada());
            lineaRecepcion.setCostoUnitario(requestLine.getCostoUnitario());
            lineaRecepcion.setObservacion(requestLine.getObservacion() == null
                    ? null : requestLine.getObservacion().trim());
            recepcion.getLineas().add(lineaRecepcion);
            lineaCompra.setCostoUnitario(requestLine.getCostoUnitario());
        }

        productos.values().forEach(productoRepository::save);

        recepcionRepository.saveAndFlush(recepcion);
        recepcion.setFolio(String.format("REC-%010d", recepcion.getId()));
        recepcionRepository.save(recepcion);
        boolean completa = compra.getLineas().stream()
                .allMatch(linea -> linea.getCantidadRecibida().equals(linea.getCantidadSolicitada()));
        boolean parcial = compra.getLineas().stream().anyMatch(linea -> linea.getCantidadRecibida() > 0);
        compra.setEstado(completa ? Compra.Estado.RECIBIDA
                : parcial ? Compra.Estado.PARCIAL : Compra.Estado.PENDIENTE);
        compraRepository.save(compra);
        log.info("Recepción registrada: compra={}, recepción={}, usuario={}",
                compra.getFolio(), recepcion.getFolio(), username);
        return toDTO(compra);
    }

    private CompraDTO toDTO(Compra compra) {
        CompraDTO dto = new CompraDTO();
        dto.setId(compra.getId());
        dto.setFolio(compra.getFolio());
        dto.setEstado(compra.getEstado().name());
        dto.setFecha(compra.getFecha());
        dto.setIdProveedor(compra.getProveedor().getId());
        dto.setNombreProveedor(compra.getProveedor().getNombre());
        dto.setUsuario(compra.getUsuario().getUsername());
        dto.setLineas(compra.getLineas().stream().map(linea -> {
            CompraDTO.LineaDTO lineaDTO = new CompraDTO.LineaDTO();
            lineaDTO.setId(linea.getId());
            lineaDTO.setIdProducto(linea.getProducto().getId());
            lineaDTO.setCodigoProducto(linea.getProducto().getCodigo());
            lineaDTO.setNombreProducto(linea.getProducto().getNombre());
            lineaDTO.setCantidadSolicitada(linea.getCantidadSolicitada());
            lineaDTO.setCantidadRecibida(linea.getCantidadRecibida());
            lineaDTO.setCostoUnitario(linea.getCostoUnitario());
            return lineaDTO;
        }).toList());
        dto.setRecepciones(recepcionRepository.findByCompraIdOrderByFechaAsc(compra.getId())
                .stream().map(this::toRecepcionDTO).toList());
        return dto;
    }

    private CompraDTO.RecepcionDTO toRecepcionDTO(RecepcionCompra recepcion) {
        CompraDTO.RecepcionDTO dto = new CompraDTO.RecepcionDTO();
        dto.setId(recepcion.getId());
        dto.setFolio(recepcion.getFolio());
        dto.setFecha(recepcion.getFecha());
        dto.setUsuario(recepcion.getUsuario().getUsername());
        dto.setLineas(recepcion.getLineas().stream().map(linea -> {
            CompraDTO.LineaRecepcionDTO lineaDTO = new CompraDTO.LineaRecepcionDTO();
            lineaDTO.setIdLineaCompra(linea.getLineaCompra().getId());
            lineaDTO.setProducto(linea.getLineaCompra().getProducto().getNombre());
            lineaDTO.setCantidadBuena(linea.getCantidadBuena());
            lineaDTO.setCantidadDanada(linea.getCantidadDanada());
            lineaDTO.setCostoUnitario(linea.getCostoUnitario());
            lineaDTO.setObservacion(linea.getObservacion());
            return lineaDTO;
        }).toList());
        return dto;
    }
}
