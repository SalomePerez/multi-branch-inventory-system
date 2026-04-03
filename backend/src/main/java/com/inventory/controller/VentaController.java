package com.inventory.controller;

import com.inventory.dto.VentaRequest;
import com.inventory.dto.VentaResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<VentaResponse>> listarTodas() {
        return ResponseEntity.ok(ventaService.listarTodas());
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<VentaResponse>> listarPorSucursal(@PathVariable Long sucursalId) {
        securityUtils.validateSucursalAccess(sucursalId);
        return ResponseEntity.ok(ventaService.listarPorSucursal(sucursalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(req));
    }
}
