package com.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record TransferenciaRequest(
        @NotNull Long sucursalOrigenId,
        @NotNull Long sucursalDestinoId,
        @NotEmpty List<ItemTransferenciaRequest> items,
        String observaciones
) {
    public record ItemTransferenciaRequest(
            @NotNull Long productoId,
            @NotNull @Positive Integer cantidadSolicitada
    ) {}
}
