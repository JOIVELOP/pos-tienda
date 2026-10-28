package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.MovimientoCaja;
import com.pos.pos.tienda.model.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByTurnoOrderByFechaAsc(TurnoCaja turno);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m WHERE m.turno = :turno AND m.tipo = :tipo")
    BigDecimal sumMontoByTurnoAndTipo(@Param("turno") TurnoCaja turno, @Param("tipo") MovimientoCaja.Tipo tipo);
}
