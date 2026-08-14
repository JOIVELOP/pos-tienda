package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.ReporteDTO;
import com.pos.pos.tienda.dto.VentaDTO;
import com.pos.pos.tienda.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
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
    public ResponseEntity<VentaDTO> save(@Valid @RequestBody VentaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.save(dto));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<VentaDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.cancelar(id));
    }

    @GetMapping("/reporte")
    public ResponseEntity<ReporteDTO> reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(ventaService.generarReporte(inicio, fin));
    }
}
