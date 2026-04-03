package com.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AjusteInventarioRequest(
        @NotNull Long productoId,
        @NotNull Long sucursalId,
        @NotNull @Min(0) Integer cantidad,
        Long unidadMedidaId,
        @NotNull String motivo,
        @Min(0) Integer stockMinimo,
        Integer stockMaximo,
        String observaciones
) {}
