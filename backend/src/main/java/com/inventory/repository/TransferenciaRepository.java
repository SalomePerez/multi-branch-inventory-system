package com.inventory.repository;

import com.inventory.entity.Transferencia;
import com.inventory.entity.enums.EstadoTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    @Query("SELECT t FROM Transferencia t WHERE t.sucursalOrigen.id = :sucursalId " +
           "OR t.sucursalDestino.id = :sucursalId ORDER BY t.createdAt DESC")
    List<Transferencia> findBySucursal(@Param("sucursalId") Long sucursalId);

    List<Transferencia> findByEstadoOrderByCreatedAtDesc(EstadoTransferencia estado);

    @Query("SELECT t FROM Transferencia t WHERE t.estado NOT IN ('PENDIENTE', 'RECHAZADA') " +
           "AND (:estado IS NULL OR t.estado = :estado) ORDER BY t.updatedAt DESC")
    List<Transferencia> findEnviosActivos(@Param("estado") EstadoTransferencia estado);

    @Query("SELECT COUNT(t) FROM Transferencia t WHERE t.estado = :estado")
    long countByEstado(@Param("estado") EstadoTransferencia estado);
}
