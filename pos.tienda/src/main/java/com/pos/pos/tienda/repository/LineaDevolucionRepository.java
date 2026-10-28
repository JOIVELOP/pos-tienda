package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.LineaDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineaDevolucionRepository extends JpaRepository<LineaDevolucion, Long> {
    List<LineaDevolucion> findByDetalleVentaId(Long idDetalleVenta);
}
