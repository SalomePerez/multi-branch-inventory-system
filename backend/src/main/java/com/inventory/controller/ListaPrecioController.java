package com.inventory.controller;

import com.inventory.entity.ListaPrecio;
import com.inventory.service.ListaPrecioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/listas-precio")
@RequiredArgsConstructor
public class ListaPrecioController {

    private final ListaPrecioService listaPrecioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ListaPrecioService.DescuentoDto>> listarTodas() {
        return ResponseEntity.ok(listaPrecioService.listarTodasDto());
    }

    @GetMapping("/activas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ListaPrecioService.DescuentoDto>> listarActivas() {
        return ResponseEntity.ok(listaPrecioService.listarActivasDto());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ListaPrecioService.DescuentoDto> crear(@RequestBody ListaPrecioService.DescuentoRequest req) {
        return ResponseEntity.ok(listaPrecioService.crearDesdeDto(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ListaPrecioService.DescuentoDto> actualizar(@PathVariable Long id, @RequestBody ListaPrecioService.DescuentoRequest req) {
        return ResponseEntity.ok(listaPrecioService.actualizarDesdeDto(id, req));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> alternarEstado(@PathVariable Long id) {
        listaPrecioService.alternarEstado(id);
        return ResponseEntity.ok().build();
    }

}
