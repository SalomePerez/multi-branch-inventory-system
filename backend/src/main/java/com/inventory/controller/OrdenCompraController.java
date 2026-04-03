package com.inventory.controller;

import com.inventory.dto.CppResponse;
import com.inventory.dto.OrdenCompraRequest;
import com.inventory.dto.OrdenCompraResponse;
import com.inventory.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<OrdenCompraResponse>> listarTodas() {
        return ResponseEntity.ok(ordenCompraService.listarTodas());
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<OrdenCompraResponse>> listarPorSucursal(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(ordenCompraService.listarPorSucursal(sucursalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<OrdenCompraResponse> crear(@Valid @RequestBody OrdenCompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenCompraService.crear(req));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<OrdenCompraResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.aprobar(id));
    }

    @PatchMapping("/{id}/recibir")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')")
    public ResponseEntity<OrdenCompraResponse> recibir(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.recibir(id));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<OrdenCompraResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.cancelar(id));
    }

    // ---- Histórico ----

    @GetMapping("/historico/proveedor")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<OrdenCompraResponse>> historicoPorProveedor(@RequestParam String nombre) {
        return ResponseEntity.ok(ordenCompraService.historicoPorProveedor(nombre));
    }

    @GetMapping("/historico/producto/{productoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<OrdenCompraResponse>> historicoPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(ordenCompraService.historicoPorProducto(productoId));
    }

    // ---- CPP ----

    @GetMapping("/cpp/{productoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<CppResponse> calcularCpp(@PathVariable Long productoId) {
        return ResponseEntity.ok(ordenCompraService.calcularCpp(productoId));
    }
}
