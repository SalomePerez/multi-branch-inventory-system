package com.inventory.dto;

import java.time.LocalDateTime;

public record DespachoRequest(
        String transportista,
        LocalDateTime fechaEstimadaLlegada,
        String prioridad,
        String rutaNombre,
        Double costoEnvio,
        Integer tiempoTransitoEstimado,
        String tipoRuta,
        String notas
) {}
