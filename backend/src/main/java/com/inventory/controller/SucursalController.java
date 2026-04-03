package com.inventory.controller;

import com.inventory.dto.SucursalDTO;
import com.inventory.entity.Sucursal;
import com.inventory.repository.SucursalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalRepository sucursalRepository;

    @GetMapping
    public ResponseEntity<List<SucursalDTO>> listar() {
        return ResponseEntity.ok(
                sucursalRepository.findByActivaTrue().stream()
                        .map(s -> new SucursalDTO(s.getId(), s.getNombre(), s.getDireccion(),
                                s.getTelefono(), s.getEmail(), s.getActiva()))
                        .toList()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SucursalDTO> crear(@Valid @RequestBody SucursalDTO dto) {
        Sucursal s = sucursalRepository.save(
                Sucursal.builder()
                        .nombre(dto.nombre())
                        .direccion(dto.direccion())
                        .telefono(dto.telefono())
                        .email(dto.email())
                        .build()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SucursalDTO(s.getId(), s.getNombre(), s.getDireccion(),
                        s.getTelefono(), s.getEmail(), s.getActiva()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SucursalDTO> actualizar(@PathVariable Long id, @Valid @RequestBody SucursalDTO dto) {
        Sucursal s = sucursalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        s.setNombre(dto.nombre());
        s.setDireccion(dto.direccion());
        s.setTelefono(dto.telefono());
        s.setEmail(dto.email());
        s = sucursalRepository.save(s);
        return ResponseEntity.ok(new SucursalDTO(s.getId(), s.getNombre(), s.getDireccion(),
                s.getTelefono(), s.getEmail(), s.getActiva()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id, @RequestParam String motivo) {
        sucursalRepository.findById(id).ifPresent(s -> {
            s.setActiva(false);
            s.setMotivoDesactivacion(motivo);
            sucursalRepository.save(s);
        });
        return ResponseEntity.noContent().build();
    }
}
