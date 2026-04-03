package com.inventory.controller;

import com.inventory.dto.InventarioResponse;
import com.inventory.dto.MovimientoResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final SecurityUtils securityUtils;

    @GetMapping("/stock/{sucursalId}")
    public ResponseEntity<List<InventarioResponse>> stockActual(@PathVariable Long sucursalId) {
        securityUtils.validateSucursalAccess(sucursalId);
        return ResponseEntity.ok(reporteService.stockActualPorSucursal(sucursalId));
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<InventarioResponse>> stockBajo() {
        return ResponseEntity.ok(reporteService.productosBajoStock());
    }

    @GetMapping("/movimientos/{sucursalId}")
    public ResponseEntity<List<MovimientoResponse>> movimientos(
            @PathVariable Long sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        securityUtils.validateSucursalAccess(sucursalId);
        return ResponseEntity.ok(reporteService.movimientosPorPeriodo(sucursalId, desde, hasta));
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<MovimientoResponse>> movimientosTodas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.movimientosTodas(desde, hasta));
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<Map<String, Object>> resumenVentasTodas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.resumenVentasTodas(desde, hasta));
    }

    @GetMapping("/ventas/{sucursalId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<Map<String, Object>> resumenVentas(
            @PathVariable Long sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        securityUtils.validateSucursalAccess(sucursalId);
        return ResponseEntity.ok(reporteService.resumenVentasPorPeriodo(sucursalId, desde, hasta));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> rankingSucursales() {
        return ResponseEntity.ok(reporteService.rankingSucursales());
    }

    @GetMapping("/evolucion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> evolucionVentas(@RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(reporteService.evolucionVentas(dias));
    }

    @GetMapping("/demanda")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> demandaProductos(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(reporteService.productosMasVendidos(limit));
    }

    @GetMapping("/mensual")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> evolucionMensual(
            @RequestParam(defaultValue = "6") int meses,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) Long productoId) {
        return ResponseEntity.ok(reporteService.evolucionMensual(meses, sucursalId, productoId));
    }

    @GetMapping("/prediccion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> prediccionDemanda(
            @RequestParam(defaultValue = "6") int mesesHistoricos,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) Long productoId) {
        return ResponseEntity.ok(reporteService.prediccionDemanda(mesesHistoricos, sucursalId, productoId));
    }

    @GetMapping("/logistica")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> resumenLogistica() {
        return ResponseEntity.ok(reporteService.resumenLogistica());
    }

    @GetMapping("/logistica/sucursal")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<Map<String, Object>>> resumenLogisticaPorSucursal() {
        return ResponseEntity.ok(reporteService.resumenLogisticaPorSucursal());
    }
}
