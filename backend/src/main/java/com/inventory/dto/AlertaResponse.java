package com.inventory.dto;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        String tipo,
        Long productoId,
        String productoNombre,
        Long sucursalId,
        String sucursalNombre,
        String mensaje,
        Boolean leida,
        LocalDateTime createdAt
) {}
