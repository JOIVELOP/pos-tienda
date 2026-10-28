package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.CompraDTO;

import java.util.List;

public interface CompraService {
    List<CompraDTO> findAll();
    CompraDTO findById(Long id);
    CompraDTO crear(CompraDTO.CrearRequest request, String username);
    CompraDTO recibir(Long id, CompraDTO.RecepcionRequest request, String username);
}
