package com.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record VentaRequest(
        @NotNull Long sucursalId,
        Long listaPrecioId,
        java.math.BigDecimal descuentoGlobal,
        @NotEmpty List<ItemVentaRequest> items,
        String observaciones
) {
    public record ItemVentaRequest(
            @NotNull Long productoId,
            @NotNull @Positive Integer cantidad,
            java.math.BigDecimal descuento
    ) {}
}
