package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.TurnoCajaDTO;

import java.util.List;
import java.util.Optional;

public interface TurnoCajaService {
    TurnoCajaDTO abrir(TurnoCajaDTO.AperturaRequest request, String username);
    Optional<TurnoCajaDTO> turnoActual(String username);
    List<TurnoCajaDTO> historial(String username);
    TurnoCajaDTO registrarMovimiento(TurnoCajaDTO.MovimientoRequest request, String username);
    TurnoCajaDTO cerrar(Long id, TurnoCajaDTO.CierreRequest request, String username);
}
