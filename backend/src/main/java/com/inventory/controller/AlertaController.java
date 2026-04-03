package com.inventory.controller;

import com.inventory.dto.AlertaResponse;
import com.inventory.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> listarNoLeidas() {
        return ResponseEntity.ok(alertaService.listarNoLeidas());
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<AlertaResponse>> listarPorSucursal(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(alertaService.listarPorSucursal(sucursalId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        return ResponseEntity.ok(Map.of("total", alertaService.contarNoLeidas()));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        alertaService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        alertaService.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }
}
