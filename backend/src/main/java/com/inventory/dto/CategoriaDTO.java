package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaDTO(
        Long id,
        @NotBlank @Size(max = 100) String nombre,
        String descripcion,
        Boolean activa
) {}
