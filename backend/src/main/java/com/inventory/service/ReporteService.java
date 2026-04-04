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

        List<VentaResponse> ventasDetalladas = ventas.stream()
                .map(this::ventaToResponse)
                .toList();

        // Use HashMap (not Map.of) to allow null values like listaPrecioId
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("sucursalId", sucursalId);
        result.put("desde", desde.toString());
        result.put("hasta", hasta.toString());
        result.put("cantidadVentas", ventas.size());
        result.put("totalVentas", totalVentas != null ? totalVentas : BigDecimal.ZERO);
        result.put("ventas", ventasDetalladas);
        return result;
    }

    public Map<String, Object> resumenVentasTodas(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        List<Venta> ventas = ventaRepository.findByPeriodo(inicio, fin);
        BigDecimal totalVentas = ventaRepository.sumTotalByPeriodo(inicio, fin);

        List<VentaResponse> ventasDetalladas = ventas.stream()
                .map(this::ventaToResponse)
                .toList();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("sucursalId", null);
        result.put("desde", desde.toString());
        result.put("hasta", hasta.toString());
        result.put("cantidadVentas", ventas.size());
        result.put("totalVentas", totalVentas != null ? totalVentas : BigDecimal.ZERO);
        result.put("ventas", ventasDetalladas);
        return result;
    }

    private VentaResponse ventaToResponse(Venta v) {
        List<VentaResponse.ItemVentaResponse> items = v.getItems().stream()
                .map(i -> new VentaResponse.ItemVentaResponse(
                        i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidad(), i.getPrecioUnitario(),
                        i.getDescuentoAplicado(), i.getSubtotal()))
                .toList();

        return new VentaResponse(
                v.getId(),
                v.getSucursal().getId(),
                v.getSucursal().getNombre(),
                v.getSucursal().getDireccion(),
                v.getSucursal().getTelefono(),
                v.getSucursal().getEmail(),
                v.getVendedor().getNombre(),
                v.getListaPrecio() != null ? v.getListaPrecio().getId() : null,
                v.getListaPrecio() != null ? v.getListaPrecio().getNombre() : null,
                v.getTotal(),
                v.getDescuentoTotal(),
                v.getObservaciones(),
                items, v.getCreatedAt()
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

    public List<Map<String, Object>> evolucionMensual(int meses, Long sucursalId, Long productoId) {
        List<Object[]> results;
        if (productoId != null) {
            if (sucursalId != null) {
                results = ventaRepository.findEvolucionMensualPorSucursalYProducto(meses, sucursalId, productoId);
            } else {
                results = ventaRepository.findEvolucionMensualPorProducto(meses, productoId);
            }
        } else if (sucursalId != null) {
            results = ventaRepository.findEvolucionMensualPorSucursal(meses, sucursalId);
        } else {
            results = ventaRepository.findEvolucionMensual(meses);
        }
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("mes", row[0]);
                    map.put("total", row[1]);
                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> prediccionDemanda(int mesesHistoricos, Long sucursalId, Long productoId) {
        List<Map<String, Object>> historico = evolucionMensual(mesesHistoricos, sucursalId, productoId);
        if (historico.size() < 2) return new ArrayList<>();

        // x = índice de mes (1, 2, ... n), y = total ventas
        double n = historico.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        // Invertimos para que el orden sea cronológico (1=más antiguo, n=más reciente)
        List<Map<String, Object>> cronologico = new ArrayList<>(historico);
        java.util.Collections.reverse(cronologico);

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = ((Number) cronologico.get(i).get("total")).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double pendiente = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double interseccion = (sumY - pendiente * sumX) / n;

        // Proyectar próximos 2 meses
        List<Map<String, Object>> proyecciones = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();
        
        for (int i = 1; i <= 2; i++) {
            double xFuturo = n + i;
            double yPredicho = pendiente * xFuturo + interseccion;
            
            // Asegurar que no haya predicciones negativas absurdas
            yPredicho = Math.max(0, yPredicho);

            Map<String, Object> proj = new java.util.HashMap<>();
            LocalDate futureMonth = baseDate.plusMonths(i);
            proj.put("mes", futureMonth.getYear() + "-" + String.format("%02d", futureMonth.getMonthValue()));
            proj.put("total", Math.round(yPredicho * 100.0) / 100.0);
            proj.put("tipo", "PREDICCION");
            proyecciones.add(proj);
        }
        
        return proyecciones;
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
            
            long sinFaltantes = envios.stream()
                    .filter(t -> t.getFechaRealLlegada() != null)
                    .filter(t -> t.getEstado() == com.inventory.entity.enums.EstadoTransferencia.COMPLETADA)
                    .count();

            double tasaCumplimiento = (completados == 0) ? 0.0 : (double) aTiempo / completados * 100.0;
            double tasaIntegridad = (completados == 0) ? 0.0 : (double) sinFaltantes / completados * 100.0;
            
            String sucursalOrigen = envios.get(0).getSucursalOrigen().getNombre();

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("ruta", ruta);
            map.put("sucursalOrigen", sucursalOrigen);
            map.put("totalEnvios", envios.size());
            map.put("completados", completados);
            map.put("aTiempo", aTiempo);
            map.put("sinFaltantes", sinFaltantes);
            map.put("tasaCumplimiento", Math.round(tasaCumplimiento));
            map.put("tasaIntegridad", Math.round(tasaIntegridad));
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
            
            long sinFaltantes = envios.stream()
                    .filter(t -> t.getFechaRealLlegada() != null)
                    .filter(t -> t.getEstado() == com.inventory.entity.enums.EstadoTransferencia.COMPLETADA)
                    .count();

            double tasa = (completados == 0) ? 0.0 : (double) aTiempo / completados * 100.0;
            double tasaCalidad = (completados == 0) ? 0.0 : (double) sinFaltantes / completados * 100.0;
            
            long rutas = envios.stream().filter(t -> t.getRutaNombre() != null)
                    .map(com.inventory.entity.Transferencia::getRutaNombre)
                    .distinct().count();

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("sucursal", sucursal);
            map.put("totalEnvios", envios.size());
            map.put("completados", completados);
            map.put("aTiempo", aTiempo);
            map.put("sinFaltantes", sinFaltantes);
            map.put("rutasActivas", rutas);
            map.put("tasaCumplimiento", Math.round(tasa));
            map.put("tasaCalidad", Math.round(tasaCalidad));
            return map;
        }).collect(Collectors.toList());
    }

    private MovimientoResponse movimientoToResponse(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getTipo().name(),
                m.getProducto() != null ? m.getProducto().getId() : null,
                m.getProducto() != null ? m.getProducto().getNombre() : null,
                m.getSucursal() != null ? m.getSucursal().getId() : null,
                m.getSucursal() != null ? m.getSucursal().getNombre() : "Global",
                m.getCantidad(), m.getCantidadAntes(), m.getCantidadDespues(),
                m.getReferenciaId(), m.getReferenciaTipo(),
                m.getUsuario() != null ? m.getUsuario().getNombre() : "Sistema",
                m.getMotivo(),
                m.getObservaciones(),
                m.getCreatedAt()
        );
    }
}
