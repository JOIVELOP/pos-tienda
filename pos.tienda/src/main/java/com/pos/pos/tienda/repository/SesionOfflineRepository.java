package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.SesionOffline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionOfflineRepository extends JpaRepository<SesionOffline, String> {
    Optional<SesionOffline> findByTurnoId(Long idTurno);
    boolean existsByTurnoUsuarioAperturaUsernameAndCerradaFalse(String username);
}
