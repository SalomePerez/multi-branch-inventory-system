package com.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransferenciaResponse(
        Long id,
        Long sucursalOrigenId,
        String sucursalOrigenNombre,
        Long sucursalDestinoId,
        String sucursalDestinoNombre,
        String estado,
        String solicitadoPorNombre,
        String aprobadoPorNombre,
        String observaciones,
        List<ItemTransferenciaResponse> items,
        LocalDateTime fechaSalida,
        LocalDateTime fechaEstimadaLlegada,
        LocalDateTime fechaRealLlegada,
        String prioridad,
        String rutaNombre,
        Double costoEnvio,
        String transportista,
        Integer tiempoTransitoEstimado,
        String tipoRuta,
        String notasDespacho,
        String operadorDespacho,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String motivoRechazo,
        String accionFaltante,
        String notasFaltante
) {
    public record ItemTransferenciaResponse(
            Long productoId,
            String productoNombre,
            Integer cantidadSolicitada,
            Integer cantidadEnviada,
            Integer cantidadRecibida
    ) {}
}
