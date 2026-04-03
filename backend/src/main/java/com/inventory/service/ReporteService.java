package com.inventory.service;

import com.inventory.dto.InventarioResponse;
import com.inventory.dto.MovimientoResponse;
import com.inventory.dto.VentaResponse;
import com.inventory.entity.Movimiento;
import com.inventory.entity.Venta;
import com.inventory.repository.MovimientoRepository;
import com.inventory.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final InventarioService inventarioService;
    private final MovimientoRepository movimientoRepository;
    private final VentaRepository ventaRepository;
    private final com.inventory.repository.TransferenciaRepository transferenciaRepository;

    public List<InventarioResponse> stockActualPorSucursal(Long sucursalId) {
        return inventarioService.listarPorSucursal(sucursalId);
    }

    public List<InventarioResponse> productosBajoStock() {
        return inventarioService.listarStockBajo();
    }

    public List<MovimientoResponse> movimientosPorPeriodo(Long sucursalId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return movimientoRepository.findBySucursalAndPeriodo(sucursalId, inicio, fin)
                .stream().map(this::movimientoToResponse).toList();
    }

    public List<MovimientoResponse> movimientosTodas(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return movimientoRepository.findByPeriodo(inicio, fin)
                .stream().map(this::movimientoToResponse).toList();
    }

    public Map<String, Object> resumenVentasPorPeriodo(Long sucursalId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        List<Venta> ventas = ventaRepository.findBySucursalAndPeriodo(sucursalId, inicio, fin);
        BigDecimal totalVentas = ventaRepository.sumTotalBySucursalAndPeriodo(sucursalId, inicio, fin);

        return Map.of(
                "sucursalId", sucursalId,
                "desde", desde.toString(),
                "hasta", hasta.toString(),
                "cantidadVentas", ventas.size(),
                "totalVentas", totalVentas != null ? totalVentas : BigDecimal.ZERO
        );
    }

    public List<Map<String, Object>> rankingSucursales() {
        return ventaRepository.findRankingSucursales().stream()
                .map(row -> Map.of(
                        "nombre", row[0],
                        "ventas", row[1]
                )).collect(Collectors.toList());
    }

    public List<Map<String, Object>> evolucionVentas(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();
        return ventaRepository.findEvolucionVentas(desde).stream()
                .map(row -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("fecha", row[0].toString());
                    map.put("total", row[1]);
                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> productosMasVendidos(int limit) {
        return ventaRepository.findTopProductosVendidos(limit).stream()
                .map(row -> Map.of(
                        "nombre", row[0],
                        "cantidad", row[1]
                )).collect(Collectors.toList());
    }

    public List<Map<String, Object>> evolucionMensual(int meses) {
        return ventaRepository.findEvolucionMensual(meses).stream()
                .map(row -> Map.of(
                        "mes", row[0],
                        "total", row[1]
                )).collect(Collectors.toList());
    }

    public List<Map<String, Object>> resumenLogistica() {
        List<com.inventory.entity.Transferencia> todas = transferenciaRepository.findAll();

        Map<String, List<com.inventory.entity.Transferencia>> porRuta = todas.stream()
                .filter(t -> t.getRutaNombre() != null)
                .collect(Collectors.groupingBy(com.inventory.entity.Transferencia::getRutaNombre));

        return porRuta.entrySet().stream().map(entry -> {
            String ruta = entry.getKey();
            List<com.inventory.entity.Transferencia> envios = entry.getValue();
            long completados = envios.stream().filter(t -> t.getFechaRealLlegada() != null).count();
            long aTiempo = envios.stream()
                    .filter(t -> t.getFechaRealLlegada() != null && t.getFechaEstimadaLlegada() != null)
                    .filter(t -> !t.getFechaRealLlegada().isAfter(t.getFechaEstimadaLlegada()))
                    .count();

            double tasaCumplimiento = (completados == 0) ? 0.0 : (double) aTiempo / completados * 100.0;
            String sucursalOrigen = envios.get(0).getSucursalOrigen().getNombre();

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("ruta", ruta);
            map.put("sucursalOrigen", sucursalOrigen);
            map.put("totalEnvios", envios.size());
            map.put("completados", completados);
            map.put("aTiempo", aTiempo);
            map.put("tasaCumplimiento", Math.round(tasaCumplimiento));
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> resumenLogisticaPorSucursal() {
        List<com.inventory.entity.Transferencia> todas = transferenciaRepository.findAll();

        Map<String, List<com.inventory.entity.Transferencia>> porSucursal = todas.stream()
                .collect(Collectors.groupingBy(t -> t.getSucursalOrigen().getNombre()));

        return porSucursal.entrySet().stream().map(entry -> {
            String sucursal = entry.getKey();
            List<com.inventory.entity.Transferencia> envios = entry.getValue();
            long completados = envios.stream()
                    .filter(t -> t.getFechaRealLlegada() != null).count();
            long aTiempo = envios.stream()
                    .filter(t -> t.getFechaRealLlegada() != null && t.getFechaEstimadaLlegada() != null)
                    .filter(t -> !t.getFechaRealLlegada().isAfter(t.getFechaEstimadaLlegada()))
                    .count();
            double tasa = (completados == 0) ? 0.0 : (double) aTiempo / completados * 100.0;
            long rutas = envios.stream().filter(t -> t.getRutaNombre() != null)
                    .map(com.inventory.entity.Transferencia::getRutaNombre)
                    .distinct().count();

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("sucursal", sucursal);
            map.put("totalEnvios", envios.size());
            map.put("completados", completados);
            map.put("aTiempo", aTiempo);
            map.put("rutasActivas", rutas);
            map.put("tasaCumplimiento", Math.round(tasa));
            return map;
        }).collect(Collectors.toList());
    }

    private MovimientoResponse movimientoToResponse(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getTipo().name(),
                m.getProducto().getId(), m.getProducto().getNombre(),
                m.getSucursal().getId(), m.getSucursal().getNombre(),
                m.getCantidad(), m.getCantidadAntes(), m.getCantidadDespues(),
                m.getReferenciaId(), m.getReferenciaTipo(),
                m.getUsuario() != null ? m.getUsuario().getNombre() : null,
                m.getMotivo(),
                m.getObservaciones(),
                m.getCreatedAt()
        );
    }
}
