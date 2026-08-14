package com.pos.pos.tienda.controller;

import com.pos.pos.tienda.dto.AuthDTO;
import com.pos.pos.tienda.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.Response> login(@Valid @RequestBody AuthDTO.Login dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
