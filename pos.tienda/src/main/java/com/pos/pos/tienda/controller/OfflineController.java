package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.OfflineDTO;
import com.pos.pos.tienda.service.impl.OfflineService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offline")
public class OfflineController {
    private final OfflineService service;

    public OfflineController(OfflineService service) { this.service = service; }

    @PostMapping("/preparar")
    public OfflineDTO.Sesion preparar(@Valid @RequestBody OfflineDTO.Preparar request, Authentication auth) {
        return service.preparar(request, auth.getName());
    }

    @PostMapping("/sincronizar")
    public OfflineDTO.Confirmacion sincronizar(@Valid @RequestBody OfflineDTO.Operacion request,
                                               Authentication auth) {
        return service.sincronizar(request, auth.getName());
    }
}
