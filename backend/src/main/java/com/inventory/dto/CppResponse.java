package com.inventory.dto;

import java.math.BigDecimal;

public record CppResponse(
        Long productoId,
        String productoNombre,
        BigDecimal cpp,
        BigDecimal totalUnidades,
        BigDecimal totalInvertido
) {}
