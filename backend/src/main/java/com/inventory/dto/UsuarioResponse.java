package com.inventory.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Long sucursalId,
        String sucursalNombre,
        Boolean activo,
        String motivoEstado,
        LocalDateTime createdAt
) {}
