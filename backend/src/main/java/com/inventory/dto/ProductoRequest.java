package com.inventory.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotNull Long categoriaId,
        @NotNull @DecimalMin("0.0") BigDecimal precioCosto,
        @NotNull @DecimalMin("0.0") BigDecimal precioVenta,
        Long unidadMedidaId
) {}
