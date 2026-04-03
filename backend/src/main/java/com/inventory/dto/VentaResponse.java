package com.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String vendedorNombre,
        Long listaPrecioId,
        String listaPrecioNombre,
        BigDecimal total,
        BigDecimal descuentoTotal,
        String observaciones,
        List<ItemVentaResponse> items,
        LocalDateTime createdAt
) {
    public record ItemVentaResponse(
            Long productoId,
            String productoNombre,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuentoAplicado,
            BigDecimal subtotal
    ) {}
}
