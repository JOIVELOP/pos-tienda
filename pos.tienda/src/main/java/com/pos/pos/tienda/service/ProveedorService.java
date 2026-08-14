package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.ProveedorDTO;

import java.util.List;

public interface ProveedorService {
    List<ProveedorDTO> findAll();
    ProveedorDTO findById(Long id);
    ProveedorDTO save(ProveedorDTO dto);
    ProveedorDTO update(Long id, ProveedorDTO dto);
    void delete(Long id);
}
