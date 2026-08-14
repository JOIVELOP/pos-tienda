package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.ConfigNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigNegocioRepository extends JpaRepository<ConfigNegocio, Long> {
    // Solo habrá un registro, se obtiene con findById(1L)
}
