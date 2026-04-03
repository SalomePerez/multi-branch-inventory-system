package com.inventory.repository;

import com.inventory.entity.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"categorias"})
    List<ListaPrecio> findAll();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"categorias"})
    List<ListaPrecio> findByActivaTrue();

    Optional<ListaPrecio> findByNombre(String nombre);
}
