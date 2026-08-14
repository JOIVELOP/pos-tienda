package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByFolio(String folio);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Venta> findByUsuarioId(Long idUsuario);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA'")
    BigDecimal totalVentasPorPeriodo(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT FUNCTION('DATE', v.fecha), COUNT(v), SUM(v.total) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA' " +
            "GROUP BY FUNCTION('DATE', v.fecha) " +
            "ORDER BY FUNCTION('DATE', v.fecha)")
    List<Object[]> reporteVentasPorDia(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT v.folio FROM Venta v ORDER BY v.id DESC LIMIT 1")
    Optional<String> findUltimoFolio();

    @Query("SELECT HOUR(v.fecha), COUNT(v), SUM(v.total) FROM Venta v " +
            "WHERE v.fecha BETWEEN :inicio AND :fin " +
            "AND v.estatus = 'COMPLETADA' " +
            "GROUP BY HOUR(v.fecha) ORDER BY HOUR(v.fecha)")
    List<Object[]> reporteVentasPorHora(LocalDateTime inicio, LocalDateTime fin);
}