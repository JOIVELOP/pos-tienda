package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.ClienteDTO;

import java.util.List;

public interface ClienteService {
    List<ClienteDTO> findAll();
    ClienteDTO findById(Long id);
    List<ClienteDTO> buscarPorNombre(String nombre);
    ClienteDTO save(ClienteDTO dto);
    ClienteDTO update(Long id, ClienteDTO dto);
    void delete(Long id);
}
