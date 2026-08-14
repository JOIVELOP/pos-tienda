package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.UsuarioDTO;

import java.util.List;

public interface UsuarioService {
    List<UsuarioDTO> findAll();
    UsuarioDTO findById(Long id);
    UsuarioDTO save(UsuarioDTO dto);
    UsuarioDTO update(Long id, UsuarioDTO dto);
    void delete(Long id);

}
