package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.PromocionDTO;
import com.pos.pos.tienda.service.PromocionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    public ResponseEntity<List<PromocionDTO>> findAll() {
        return ResponseEntity.ok(promocionService.findAll());
    }

    @GetMapping("/vigentes")
    public ResponseEntity<List<PromocionDTO>> findVigentes() {
        return ResponseEntity.ok(promocionService.findVigentes());
    }

    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<List<PromocionDTO>> findByProducto(@PathVariable Long idProducto) {
        return ResponseEntity.ok(promocionService.findByProducto(idProducto));
    }

    @PostMapping
    public ResponseEntity<PromocionDTO> save(@Valid @RequestBody PromocionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promocionService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocionDTO> update(@PathVariable Long id,
                                               @Valid @RequestBody PromocionDTO dto) {
        return ResponseEntity.ok(promocionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promocionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/precio/{idProducto}")
    public ResponseEntity<Map<String, Object>> getPrecioConPromocion(@PathVariable Long idProducto) {
        return ResponseEntity.ok(promocionService.calcularPrecioConPromocion(idProducto));
    }
}
