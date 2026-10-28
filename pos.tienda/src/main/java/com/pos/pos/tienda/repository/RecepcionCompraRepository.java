package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.RecepcionCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecepcionCompraRepository extends JpaRepository<RecepcionCompra, Long> {
    List<RecepcionCompra> findByCompraIdOrderByFechaAsc(Long idCompra);
}
