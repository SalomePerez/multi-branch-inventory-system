package com.inventory.repository;

import com.inventory.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByCreatedAtDesc();
    List<Venta> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);

    @Query("SELECT v FROM Venta v WHERE v.sucursal.id = :sucursalId " +
           "AND v.createdAt BETWEEN :desde AND :hasta ORDER BY v.createdAt DESC")
    List<Venta> findBySucursalAndPeriodo(
            @Param("sucursalId") Long sucursalId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.sucursal.id = :sucursalId " +
           "AND v.createdAt BETWEEN :desde AND :hasta")
    BigDecimal sumTotalBySucursalAndPeriodo(
            @Param("sucursalId") Long sucursalId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s.nombre as sucursal, SUM(v.total) as total " +
           "FROM Venta v JOIN v.sucursal s " +
           "GROUP BY s.nombre ORDER BY SUM(v.total) DESC")
    List<Object[]> findRankingSucursales();

    @Query("SELECT CAST(v.createdAt AS LocalDate) as fecha, SUM(v.total) as total " +
           "FROM Venta v " +
           "WHERE v.createdAt >= :desde " +
           "GROUP BY CAST(v.createdAt AS LocalDate) " +
           "ORDER BY CAST(v.createdAt AS LocalDate) ASC")
    List<Object[]> findEvolucionVentas(@Param("desde") LocalDateTime desde);

    @Query(value = "SELECT p.nombre, SUM(vi.cantidad) as total " +
           "FROM venta_items vi " +
           "JOIN productos p ON vi.producto_id = p.id " +
           "GROUP BY p.nombre " +
           "ORDER BY total DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopProductosVendidos(@Param("limit") int limit);

    @Query(value = "SELECT TO_CHAR(v.created_at, 'YYYY-MM') as mes, SUM(v.total) as total " +
                   "FROM ventas v " +
                   "GROUP BY mes ORDER BY mes DESC LIMIT :meses", nativeQuery = true)
    List<Object[]> findEvolucionMensual(@Param("meses") int meses);
}
