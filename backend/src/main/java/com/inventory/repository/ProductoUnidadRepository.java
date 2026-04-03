package com.inventory.repository;

import com.inventory.entity.ProductoUnidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoUnidadRepository extends JpaRepository<ProductoUnidad, Long> {
    List<ProductoUnidad> findByProductoId(Long productoId);
    Optional<ProductoUnidad> findByProductoIdAndUnidadMedidaId(Long productoId, Long unidadMedidaId);
    Optional<ProductoUnidad> findByProductoIdAndEsBaseTrue(Long productoId);
}
