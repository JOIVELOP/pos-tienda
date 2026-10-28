package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByFolio(String folio);

    List<Venta> findByFechaBetweenAndEstatus(LocalDateTime inicio, LocalDateTime fin,
                                             Venta.Estatus estatus);

    List<Venta> findByUsuarioId(Long idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA'")
    BigDecimal totalVentasPorPeriodo(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v "
            + "WHERE v.turnoCaja.id = :turnoId AND v.estatus = 'COMPLETADA' "
            + "AND v.metodoPago = 'EFECTIVO'")
    BigDecimal sumCashSalesByTurnoId(@Param("turnoId") Long turnoId);

    @Query("SELECT FUNCTION('DATE', v.fecha), COUNT(v), SUM(v.total) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA' " +
            "GROUP BY FUNCTION('DATE', v.fecha) " +
            "ORDER BY FUNCTION('DATE', v.fecha)")
    List<Object[]> reporteVentasPorDia(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT HOUR(v.fecha), COUNT(v), SUM(v.total) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA' " +
            "GROUP BY HOUR(v.fecha) ORDER BY HOUR(v.fecha)")
    List<Object[]> reporteVentasPorHora(LocalDateTime inicio, LocalDateTime fin);
}