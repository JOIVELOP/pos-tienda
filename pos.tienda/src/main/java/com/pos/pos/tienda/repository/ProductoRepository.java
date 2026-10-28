package com.pos.pos.tienda.repository;

import com.pos.pos.tienda.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    List<Producto> findByActivoTrue();
    Optional<Producto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.activo = true")
    List<Producto> findProductosBajoStock();


    @Query("SELECT p FROM Producto p WHERE p.activo = true AND " +
            "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Producto> buscarPorNombreOCodigo(String busqueda);

    List<Producto> findByCategoriaIdAndActivoTrue(Long idCategoria);
}
