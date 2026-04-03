package com.inventory.repository;

import com.inventory.entity.OrdenCompra;
import com.inventory.entity.enums.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    List<OrdenCompra> findAllByOrderByCreatedAtDesc();
    List<OrdenCompra> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);
    List<OrdenCompra> findBySucursalIdAndEstado(Long sucursalId, EstadoOrden estado);

    @Query("SELECT o FROM OrdenCompra o WHERE LOWER(o.proveedor) LIKE LOWER(CONCAT('%', :nombre, '%')) ORDER BY o.createdAt DESC")
    List<OrdenCompra> findByProveedorContainingIgnoreCase(@Param("nombre") String nombre);

    @Query("SELECT DISTINCT o FROM OrdenCompra o JOIN o.items i WHERE i.producto.id = :productoId ORDER BY o.createdAt DESC")
    List<OrdenCompra> findByProductoId(@Param("productoId") Long productoId);

    @Query("SELECT o FROM OrdenCompra o JOIN o.items i WHERE i.producto.id = :productoId AND o.estado = :estado ORDER BY o.createdAt DESC")
    List<OrdenCompra> findByProductoIdAndEstado(@Param("productoId") Long productoId, @Param("estado") EstadoOrden estado);
}
