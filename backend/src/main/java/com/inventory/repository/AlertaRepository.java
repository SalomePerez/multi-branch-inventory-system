package com.inventory.repository;

import com.inventory.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByLeidaFalseOrderByCreatedAtDesc();
    List<Alerta> findBySucursalIdAndLeidaFalse(Long sucursalId);
    long countByLeidaFalse();
}
