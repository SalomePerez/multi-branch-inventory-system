package com.inventory.dto;

import java.time.LocalDateTime;

public record InventarioResponse(
        Long id,
        Long productoId,
        String productoSku,
        String productoNombre,
        String categoriaNombre,
        Long sucursalId,
        String sucursalNombre,
        Integer cantidad,
        String productoUnidadNombre,
        String productoUnidadAbreviatura,
        Integer stockMinimo,
        Integer stockMaximo,
        Boolean bajoCritico,
        LocalDateTime updatedAt
) {}
