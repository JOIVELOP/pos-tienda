package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.TurnoCajaDTO;
import com.pos.pos.tienda.service.TurnoCajaService;
import com.pos.pos.tienda.service.impl.OfflineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class TurnoCajaController {
    private final TurnoCajaService turnoCajaService;
    private final OfflineService offlineService;

    public TurnoCajaController(TurnoCajaService turnoCajaService, OfflineService offlineService) {
        this.turnoCajaService = turnoCajaService;
        this.offlineService = offlineService;
    }

    @GetMapping("/actual")
    public ResponseEntity<TurnoCajaDTO> actual(Authentication authentication) {
        return turnoCajaService.turnoActual(authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/historial")
    public List<TurnoCajaDTO> historial(Authentication authentication) {
        return turnoCajaService.historial(authentication.getName());
    }

    @PostMapping("/abrir")
    public ResponseEntity<TurnoCajaDTO> abrir(@Valid @RequestBody TurnoCajaDTO.AperturaRequest request,
                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(turnoCajaService.abrir(request, authentication.getName()));
    }

    @PostMapping("/movimientos")
    @Transactional
    public ResponseEntity<TurnoCajaDTO> movimiento(
            @Valid @RequestBody TurnoCajaDTO.MovimientoRequest request,
            Authentication authentication) {
        offlineService.exigirSinSesion(authentication.getName());
        return ResponseEntity.ok(turnoCajaService.registrarMovimiento(request, authentication.getName()));
    }

    @PostMapping("/{id}/cerrar")
    @Transactional
    public ResponseEntity<TurnoCajaDTO> cerrar(@PathVariable Long id,
                                                @Valid @RequestBody TurnoCajaDTO.CierreRequest request,
                                                Authentication authentication) {
        offlineService.exigirSinSesion(authentication.getName());
        return ResponseEntity.ok(turnoCajaService.cerrar(id, request, authentication.getName()));
    }
}
