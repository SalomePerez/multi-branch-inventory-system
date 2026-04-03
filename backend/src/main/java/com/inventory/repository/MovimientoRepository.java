package com.inventory.repository;

import com.inventory.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);

    List<Movimiento> findByProductoIdOrderByCreatedAtDesc(Long productoId);

    @Query("SELECT m FROM Movimiento m WHERE m.sucursal.id = :sucursalId " +
           "AND m.createdAt BETWEEN :desde AND :hasta ORDER BY m.createdAt DESC")
    List<Movimiento> findBySucursalAndPeriodo(
            @Param("sucursalId") Long sucursalId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT m FROM Movimiento m WHERE m.createdAt BETWEEN :desde AND :hasta ORDER BY m.createdAt DESC")
    List<Movimiento> findByPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
