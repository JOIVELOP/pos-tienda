package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.DevolucionVenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long> {
    List<DevolucionVenta> findByVentaIdOrderByFechaAsc(Long idVenta);
}
