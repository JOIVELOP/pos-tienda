package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.UsuarioDTO;
import com.pos.pos.tienda.model.Usuario;
import com.pos.pos.tienda.repository.UsuarioRepository;
import com.pos.pos.tienda.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado: ";
    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UsuarioDTO> findAll() {
        return usuarioRepository.findByActivoTrue()
                .stream().map(this::toDTO).toList();
    }

    @Override
    public UsuarioDTO findById(Long id) {
        return toDTO(usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(USUARIO_NO_ENCONTRADO + id)));
    }

    @Override
    public UsuarioDTO save(UsuarioDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("Ya existe un usuario con ese username");
        }

        Usuario usuario = toEntity(dto);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario creado: username={}, rol={}",
                usuarioGuardado.getUsername(), usuarioGuardado.getRol());
        return toDTO(usuarioGuardado);
    }


    @Override
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(USUARIO_NO_ENCONTRADO + id));
        usuario.setNombre(dto.getNombre());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return toDTO(usuarioRepository.save(usuario));
    }

    @Override
    public void delete(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(USUARIO_NO_ENCONTRADO + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioDTO toDTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setUsername(u.getUsername());
        dto.setRol(u.getRol());
        dto.setActivo(u.getActivo());
        return dto;
    }

    private Usuario toEntity(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setUsername(dto.getUsername());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        return usuario;
    }
}
