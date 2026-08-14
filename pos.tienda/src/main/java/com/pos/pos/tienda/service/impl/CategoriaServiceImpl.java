package com.pos.pos.tienda.service.impl;

import com.pos.pos.tienda.config.BusinessException;
import com.pos.pos.tienda.dto.CategoriaDTO;
import com.pos.pos.tienda.model.Categoria;
import com.pos.pos.tienda.repository.CategoriaRepository;
import com.pos.pos.tienda.service.CategoriaService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final String CATEGORIA_NO_ENCONTRADA = "Categoría no encontrada con id: ";

    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public List<CategoriaDTO> findAll() {
        return categoriaRepository.findByActivoTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public CategoriaDTO findById(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CATEGORIA_NO_ENCONTRADA + id));
        return toDTO(categoria);
    }

    @Override
    public CategoriaDTO save(CategoriaDTO dto) {
        if (categoriaRepository.existsByNombre(dto.getNombre())) {
            throw new BusinessException("Ya existe una categoría con ese nombre");
        }
        return toDTO(categoriaRepository.save(toEntity(dto)));
    }

    @Override
    public CategoriaDTO update(Long id, CategoriaDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CATEGORIA_NO_ENCONTRADA + id)); // ✅
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setActivo(dto.getActivo());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Override
    public void delete(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CATEGORIA_NO_ENCONTRADA + id)); // ✅
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private CategoriaDTO toDTO(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setActivo(categoria.getActivo());
        return dto;
    }

    private Categoria toEntity(CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setActivo(dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE);
        return categoria;
    }
}
