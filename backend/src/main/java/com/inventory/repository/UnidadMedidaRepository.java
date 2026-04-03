package com.inventory.repository;

import com.inventory.entity.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    Optional<UnidadMedida> findByNombre(String nombre);
    Optional<UnidadMedida> findByAbreviatura(String abreviatura);
}
