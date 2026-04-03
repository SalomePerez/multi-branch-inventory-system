package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal precioCosto,
        BigDecimal precioVenta,
        String unidadMedidaNombre,
        String unidadMedidaAbreviatura,
        Boolean activo,
        LocalDateTime createdAt
) {}
