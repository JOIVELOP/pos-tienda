package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.DevolucionDTO;
import com.pos.pos.tienda.service.DevolucionService;
import com.pos.pos.tienda.service.impl.OfflineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {
    private final DevolucionService devolucionService;
    private final OfflineService offlineService;

    public DevolucionController(DevolucionService devolucionService, OfflineService offlineService) {
        this.devolucionService = devolucionService;
        this.offlineService = offlineService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DevolucionDTO> crear(@Valid @RequestBody DevolucionDTO.CrearRequest request,
                                                Authentication authentication) {
        offlineService.exigirSinSesion(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devolucionService.crear(request, authentication.getName()));
    }

    @GetMapping("/venta/{idVenta}")
    public List<DevolucionDTO> findByVenta(@PathVariable Long idVenta) {
        return devolucionService.findByVenta(idVenta);
    }
}
