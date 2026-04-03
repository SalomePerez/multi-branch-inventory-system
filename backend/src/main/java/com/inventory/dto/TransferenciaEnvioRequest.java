package com.inventory.dto;

import java.util.List;

public record TransferenciaEnvioRequest(
    List<Integer> cantidadesEnviadas,
    String transportista
) {}
