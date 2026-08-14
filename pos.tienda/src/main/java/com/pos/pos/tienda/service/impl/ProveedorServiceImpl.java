package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.ProveedorDTO;
import com.pos.pos.tienda.model.Proveedor;
import com.pos.pos.tienda.repository.ProveedorRepository;
import com.pos.pos.tienda.service.ProveedorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private static final String PROVEEDOR_NO_ENCONTRADO = "Proveedor no encontrado: ";

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<ProveedorDTO> findAll() {
        return proveedorRepository.findByActivoTrue()
                .stream().map(this::toDTO).toList();
    }

    @Override
    public ProveedorDTO findById(Long id) {
        return toDTO(proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PROVEEDOR_NO_ENCONTRADO + id)));
    }

    @Override
    public ProveedorDTO save(ProveedorDTO dto) {
        if (dto.getRfc() != null && proveedorRepository.existsByRfc(dto.getRfc())) {
            throw new BusinessException("Ya existe un proveedor con ese RFC");
        }
        return toDTO(proveedorRepository.save(toEntity(dto)));
    }

    @Override
    public ProveedorDTO update(Long id, ProveedorDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PROVEEDOR_NO_ENCONTRADO + id));
        proveedor.setNombre(dto.getNombre());
        proveedor.setRfc(dto.getRfc());
        proveedor.setTelefono(dto.getTelefono());
        proveedor.setEmail(dto.getEmail());
        proveedor.setDireccion(dto.getDireccion());
        proveedor.setActivo(dto.getActivo());
        return toDTO(proveedorRepository.save(proveedor));
    }

    @Override
    public void delete(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PROVEEDOR_NO_ENCONTRADO + id));
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    private ProveedorDTO toDTO(Proveedor p) {
        ProveedorDTO dto = new ProveedorDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setRfc(p.getRfc());
        dto.setTelefono(p.getTelefono());
        dto.setEmail(p.getEmail());
        dto.setDireccion(p.getDireccion());
        dto.setActivo(p.getActivo());
        return dto;
    }

    private Proveedor toEntity(ProveedorDTO dto) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre());
        proveedor.setRfc(dto.getRfc());
        proveedor.setTelefono(dto.getTelefono());
        proveedor.setEmail(dto.getEmail());
        proveedor.setDireccion(dto.getDireccion());
        proveedor.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        return proveedor;
    }

}
