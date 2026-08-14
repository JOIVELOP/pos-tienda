package com.pos.pos.tienda.service;

import com.pos.pos.tienda.dto.PromocionDTO;

import java.util.List;
import java.util.Map;

public interface PromocionService {
    List<PromocionDTO> findAll();
    List<PromocionDTO> findVigentes();
    List<PromocionDTO> findByProducto(Long idProducto);
    PromocionDTO save(PromocionDTO dto);
    PromocionDTO update(Long id, PromocionDTO dto);
    Map<String, Object> calcularPrecioConPromocion(Long idProducto);
    void delete(Long id);

}
