package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActivoTrue();
    Optional<Proveedor> findByRfc(String rfc);
    boolean existsByRfc(String rfc);
}