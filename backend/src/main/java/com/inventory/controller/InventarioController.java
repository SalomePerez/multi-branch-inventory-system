package com.inventory.controller;

import com.inventory.dto.AjusteInventarioRequest;
import com.inventory.dto.InventarioResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<InventarioResponse>> listarTodo() {
        return ResponseEntity.ok(inventarioService.listarTodo());
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<InventarioResponse>> listarPorSucursal(@PathVariable Long sucursalId) {
        securityUtils.validateSucursalAccess(sucursalId);
        return ResponseEntity.ok(inventarioService.listarPorSucursal(sucursalId));
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<InventarioResponse>> stockBajo() {
        return ResponseEntity.ok(inventarioService.listarStockBajo());
    }

    @PostMapping("/ajustar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')")
    public ResponseEntity<InventarioResponse> ajustar(@Valid @RequestBody AjusteInventarioRequest req) {
        securityUtils.validateSucursalAccess(req.sucursalId());
        return ResponseEntity.ok(inventarioService.ajustar(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        // Validar acceso antes de eliminar
        // Primero obtenemos el inventario para saber de qué sucursal es
        inventarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
