package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.AuthDTO;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.security.JwtUtil;
import com.pos.pos.tienda.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

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
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos"));

        System.out.println("USERNAME: " + dto.getUsername());
        System.out.println("PASSWORD: " + dto.getPassword());



        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("Usuario inactivo");
        }

        String token = jwtUtil.generateToken(
                usuario.getUsername(),
                usuario.getRol().name()

        );
        System.out.println("TOKEN: " + token);

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
