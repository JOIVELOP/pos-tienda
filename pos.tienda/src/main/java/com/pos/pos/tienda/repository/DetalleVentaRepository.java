package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    List<DetalleVenta> findByVentaId(Long idVenta);


    @Query("SELECT d.producto.nombre, SUM(d.cantidad), SUM(d.subtotal) " +
            "FROM DetalleVenta d " +
            "JOIN d.venta v " +
            "WHERE v.estatus = 'COMPLETADA' " +
            "GROUP BY d.producto.nombre " +
            "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> productosMasVendidos();

}
