package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    List<Promocion> findByActivoTrue();
    List<Promocion> findByProductoId(Long idProducto);

    @Query("SELECT p FROM Promocion p WHERE p.activo = true " +
            "AND (p.fechaInicio IS NULL OR p.fechaInicio <= :hoy) " +
            "AND (p.fechaFin IS NULL OR p.fechaFin >= :hoy)")
    List<Promocion> findPromocionesVigentes(LocalDate hoy);

    @Query("SELECT p FROM Promocion p WHERE p.activo = true " +
            "AND p.producto.id = :idProducto " +
            "AND (p.fechaInicio IS NULL OR p.fechaInicio <= :hoy) " +
            "AND (p.fechaFin IS NULL OR p.fechaFin >= :hoy)")
    List<Promocion> findPromocionVigenteByProducto(Long idProducto, LocalDate hoy);
}