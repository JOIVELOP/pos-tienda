package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.ProductoDTO;

import java.util.List;

public interface ProductoService {
    List<ProductoDTO> findAll();
    ProductoDTO findById(Long id);
    ProductoDTO findByCodigo(String codigo);
    List<ProductoDTO> buscar(String termino);
    List<ProductoDTO> findBajoStock();
    ProductoDTO save(ProductoDTO dto);
    ProductoDTO update(Long id, ProductoDTO dto);
    void delete(Long id);
    void actualizarStock(Long id, Integer cantidad);
}