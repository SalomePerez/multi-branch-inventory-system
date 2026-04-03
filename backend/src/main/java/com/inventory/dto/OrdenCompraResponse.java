package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCompraResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String proveedor,
        String estado,
        BigDecimal total,
        String creadoPorNombre,
        String aprobadoPorNombre,
        String observaciones,
        Integer plazoPago,
        LocalDateTime fechaVencimientoPago,
        List<ItemOrdenResponse> items,
        LocalDateTime createdAt
) {
    public record ItemOrdenResponse(
            Long productoId,
            String productoNombre,
            Integer cantidad,
            BigDecimal precioUnitario
    ) {}
}
