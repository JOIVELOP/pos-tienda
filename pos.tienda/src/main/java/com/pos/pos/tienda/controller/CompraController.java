package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.CompraDTO;
import com.pos.pos.tienda.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {
    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public List<CompraDTO> findAll() { return compraService.findAll(); }

    @GetMapping("/{id}")
    public CompraDTO findById(@PathVariable Long id) { return compraService.findById(id); }

    @PostMapping
    public ResponseEntity<CompraDTO> crear(@Valid @RequestBody CompraDTO.CrearRequest request,
                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compraService.crear(request, authentication.getName()));
    }

    @PostMapping("/{id}/recepciones")
    public ResponseEntity<CompraDTO> recibir(@PathVariable Long id,
                                              @Valid @RequestBody CompraDTO.RecepcionRequest request,
                                              Authentication authentication) {
        return ResponseEntity.ok(compraService.recibir(id, request, authentication.getName()));
    }
}
