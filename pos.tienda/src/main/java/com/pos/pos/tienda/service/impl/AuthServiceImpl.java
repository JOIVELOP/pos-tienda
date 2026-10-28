package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.AuthDTO;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.security.JwtUtil;
import com.pos.pos.tienda.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }


    @Override
    public AuthDTO.Response login(AuthDTO.Login dto) {
        Usuario usuario = usuarioRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> {
                    log.warn("Inicio de sesión rechazado: usuario no encontrado={}", dto.getUsername());
                    return new BusinessException("Usuario o contraseña incorrectos");
                });

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            log.warn("Inicio de sesión rechazado: contraseña incorrecta para usuario={}",
                    dto.getUsername());
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Inicio de sesión rechazado: usuario inactivo={}", dto.getUsername());
            throw new BusinessException("Usuario inactivo");
        }

        String token = jwtUtil.generateToken(
                usuario.getUsername(),
                usuario.getRol().name()

        );
        log.info("Inicio de sesión exitoso para usuario={}", usuario.getUsername());

        return new AuthDTO.Response(
                token,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol().name()
        );
    }
}
