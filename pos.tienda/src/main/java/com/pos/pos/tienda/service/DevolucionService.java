package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.DevolucionDTO;

import java.util.List;

public interface DevolucionService {
    DevolucionDTO crear(DevolucionDTO.CrearRequest request, String username);
    List<DevolucionDTO> findByVenta(Long idVenta);
}
