package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.AuthDTO;

public interface AuthService {
    AuthDTO.Response login(AuthDTO.Login dto);
}
