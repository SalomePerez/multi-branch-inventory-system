package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrdenCompraRequest(
        @NotNull Long sucursalId,
        @NotBlank String proveedor,
        @NotEmpty List<ItemOrdenRequest> items,
        String observaciones,
        Integer plazoPago
) {
    public record ItemOrdenRequest(
            @NotNull Long productoId,
            @NotNull @Positive Integer cantidad,
            @NotNull @Positive BigDecimal precioUnitario
    ) {}
}
