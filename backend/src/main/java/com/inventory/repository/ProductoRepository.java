package com.inventory.repository;

import com.inventory.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
    Optional<Producto> findBySku(String sku);
    boolean existsBySku(String sku);

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.activo = true")
    List<Producto> findAllActivosConCategoria();
}
