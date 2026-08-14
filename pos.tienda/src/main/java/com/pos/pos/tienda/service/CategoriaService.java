package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.CategoriaDTO;

import java.util.List;

public interface CategoriaService {
    List<CategoriaDTO> findAll();
    CategoriaDTO findById(Long id);
    CategoriaDTO save(CategoriaDTO dto);
    CategoriaDTO update(Long id, CategoriaDTO dto);
    void delete(Long id);
}