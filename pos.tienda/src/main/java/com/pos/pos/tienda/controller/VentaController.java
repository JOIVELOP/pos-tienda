package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.ReporteDTO;
import com.pos.pos.tienda.dto.VentaDTO;
import com.pos.pos.tienda.service.VentaService;
import com.pos.pos.tienda.service.impl.OfflineService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final OfflineService offlineService;

    public VentaController(VentaService ventaService, OfflineService offlineService) {
        this.ventaService = ventaService;
        this.offlineService = offlineService;
    }

    @GetMapping
    public ResponseEntity<List<VentaDTO>> findAll() {
        return ResponseEntity.ok(ventaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.findById(id));
    }

    @GetMapping("/folio/{folio}")
    public ResponseEntity<VentaDTO> findByFolio(@PathVariable String folio) {
        return ResponseEntity.ok(ventaService.findByFolio(folio));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<VentaDTO> save(@Valid @RequestBody VentaDTO dto,
                                         Authentication authentication) {
        offlineService.exigirSinSesion(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.save(dto, authentication.getName()));
    }

    @PatchMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<VentaDTO> cancelar(@PathVariable Long id) {
        offlineService.exigirVentaEditable(id);
        return ResponseEntity.ok(ventaService.cancelar(id));
    }

    @GetMapping("/reporte")
    public ResponseEntity<ReporteDTO> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(ventaService.generarReporte(inicio, fin));
    }
}
