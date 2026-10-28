package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.TurnoCaja;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {
    @Query("SELECT t FROM TurnoCaja t WHERE t.usuarioApertura.username = :username "
            + "AND t.estado = :estado")
    Optional<TurnoCaja> findOpenByUsername(@Param("username") String username,
                                          @Param("estado") TurnoCaja.Estado estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoCaja t WHERE t.usuarioApertura.username = :username "
            + "AND t.estado = :estado")
    Optional<TurnoCaja> findOpenByUsernameForUpdate(@Param("username") String username,
                                                   @Param("estado") TurnoCaja.Estado estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoCaja t WHERE t.id = :id")
    Optional<TurnoCaja> findByIdForUpdate(@Param("id") Long id);

    List<TurnoCaja> findByUsuarioAperturaUsernameOrderByAperturaDesc(String username);

    List<TurnoCaja> findAllByOrderByAperturaDesc();
}
