package com.inventory.dto;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        String tipo,
        Long productoId,
        String productoNombre,
        Long sucursalId,
        String sucursalNombre,
        Integer cantidad,
        Integer cantidadAntes,
        Integer cantidadDespues,
        Long referenciaId,
        String referenciaTipo,
        String usuarioNombre,
        String motivo,
        String observaciones,
        LocalDateTime createdAt
) {}
