package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SucursalDTO(
        Long id,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 255) String direccion,
        @Size(max = 20) String telefono,
        @Size(max = 100) String email,
        Boolean activa
) {}
