package com.inventory.repository;

import com.inventory.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    List<Inventario> findBySucursalId(Long sucursalId);

    @Query("SELECT i FROM Inventario i JOIN FETCH i.producto p LEFT JOIN FETCH p.categoria LEFT JOIN FETCH p.unidadMedida JOIN FETCH i.sucursal")
    List<Inventario> findAllConProducto();

    @Query("SELECT i FROM Inventario i JOIN FETCH i.producto p LEFT JOIN FETCH p.categoria LEFT JOIN FETCH p.unidadMedida JOIN FETCH i.sucursal " +
           "WHERE i.sucursal.id = :sucursalId")
    List<Inventario> findBySucursalIdConProducto(@Param("sucursalId") Long sucursalId);

    @Query("SELECT i FROM Inventario i WHERE i.cantidad <= i.stockMinimo")
    List<Inventario> findStockBajoMinimo();

    @Query("SELECT i FROM Inventario i WHERE i.cantidad <= i.stockMinimo AND i.sucursal.id = :sucursalId")
    List<Inventario> findStockBajoMinimoBySucursal(@Param("sucursalId") Long sucursalId);

    @Query("SELECT COALESCE(SUM(i.cantidad), 0) FROM Inventario i WHERE i.producto.id = :productoId")
    Integer sumCantidadByProductoId(@Param("productoId") Long productoId);
}
