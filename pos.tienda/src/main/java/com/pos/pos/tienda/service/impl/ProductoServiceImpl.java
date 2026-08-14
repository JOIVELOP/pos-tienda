package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.ProductoDTO;
import com.pos.pos.tienda.model.Producto;
import com.pos.pos.tienda.repository.CategoriaRepository;
import com.pos.pos.tienda.repository.ProductoRepository;
import com.pos.pos.tienda.repository.ProveedorRepository;
import com.pos.pos.tienda.service.ProductoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final String PRODUCTO_NO_ENCONTRADO = "Producto no encontrado: ";

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<ProductoDTO> findAll() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ProductoDTO findById(Long id) {
        return toDTO(productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PRODUCTO_NO_ENCONTRADO + id)));
    }

    @Override
    public ProductoDTO findByCodigo(String codigo) {
        return toDTO(productoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessException(PRODUCTO_NO_ENCONTRADO + codigo)));
    }

    @Override
    public List<ProductoDTO> buscar(String termino) {
        return productoRepository.buscarPorNombreOCodigo(termino)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ProductoDTO> findBajoStock() {
        return productoRepository.findProductosBajoStock()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ProductoDTO save(ProductoDTO dto) {
        if (dto.getCodigo() != null && productoRepository.existsByCodigo(dto.getCodigo())) {
            throw new BusinessException("Ya existe un producto con ese código");
        }
        return toDTO(productoRepository.save(toEntity(dto)));
    }

    @Override
    public ProductoDTO update(Long id, ProductoDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PRODUCTO_NO_ENCONTRADO + id));
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecioCompra(dto.getPrecioCompra());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setImagenUrl(dto.getImagenUrl());
        producto.setActivo(dto.getActivo());
        if (dto.getIdCategoria() != null) {
            producto.setCategoria(categoriaRepository.findById(dto.getIdCategoria()).orElse(null));
        }
        if (dto.getIdProveedor() != null) {
            producto.setProveedor(proveedorRepository.findById(dto.getIdProveedor()).orElse(null));
        }
        return toDTO(productoRepository.save(producto));
    }

    @Override
    public void delete(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PRODUCTO_NO_ENCONTRADO + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    public void actualizarStock(Long id, Integer cantidad) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PRODUCTO_NO_ENCONTRADO + id));
        producto.setStock(producto.getStock() + cantidad);
        productoRepository.save(producto);
    }

    private ProductoDTO toDTO(Producto p) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(p.getId());
        dto.setCodigo(p.getCodigo());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setPrecioCompra(p.getPrecioCompra());
        dto.setPrecioVenta(p.getPrecioVenta());
        dto.setStock(p.getStock());
        dto.setStockMinimo(p.getStockMinimo());
        dto.setImagenUrl(p.getImagenUrl());
        dto.setActivo(p.getActivo());
        if (p.getCategoria() != null) {
            dto.setIdCategoria(p.getCategoria().getId());
            dto.setNombreCategoria(p.getCategoria().getNombre());
        }
        if (p.getProveedor() != null) {
            dto.setIdProveedor(p.getProveedor().getId());
            dto.setNombreProveedor(p.getProveedor().getNombre());
        }
        return dto;
    }

    private Producto toEntity(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setCodigo(dto.getCodigo());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecioCompra(dto.getPrecioCompra());
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setStock(dto.getStock() != null ? dto.getStock() : 0);
        producto.setStockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0);
        producto.setImagenUrl(dto.getImagenUrl());
        producto.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        if (dto.getIdCategoria() != null) {
            producto.setCategoria(categoriaRepository.findById(dto.getIdCategoria()).orElse(null));
        }
        if (dto.getIdProveedor() != null) {
            producto.setProveedor(proveedorRepository.findById(dto.getIdProveedor()).orElse(null));
        }
        return producto;
    }
}