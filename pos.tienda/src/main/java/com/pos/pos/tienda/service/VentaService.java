package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.ReporteDTO;
import com.pos.pos.tienda.dto.VentaDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaService {
    List<VentaDTO> findAll();
    VentaDTO findById(Long id);
    VentaDTO findByFolio(String folio);
    VentaDTO save(VentaDTO dto, String username);
    VentaDTO cancelar(Long id);
    ReporteDTO generarReporte(LocalDateTime inicio, LocalDateTime fin);
    ReporteDTO generarReporteDetallado(LocalDateTime inicio, LocalDateTime fin);
}
