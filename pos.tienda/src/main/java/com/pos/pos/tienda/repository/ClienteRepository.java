package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByActivoTrue();

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByEmail(String email);

    boolean existsByRfc(String rfc);

    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
}
