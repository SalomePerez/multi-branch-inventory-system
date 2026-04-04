package com.inventory.controller;

import com.inventory.dto.TransferenciaEnvioRequest;
import com.inventory.dto.TransferenciaRequest;
import com.inventory.dto.TransferenciaResponse;
import com.inventory.dto.RechazarRequest;
import com.inventory.service.TransferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<List<TransferenciaResponse>> listarTodas() {
        return ResponseEntity.ok(transferenciaService.listarTodas());
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<TransferenciaResponse>> listarPorSucursal(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(transferenciaService.listarPorSucursal(sucursalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferenciaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(transferenciaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TransferenciaResponse> solicitar(@Valid @RequestBody TransferenciaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferenciaService.solicitar(req));
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<TransferenciaResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(transferenciaService.aprobar(id));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL')")
    public ResponseEntity<TransferenciaResponse> rechazar(@PathVariable Long id,
                                                           @RequestBody RechazarRequest req) {
        return ResponseEntity.ok(transferenciaService.rechazar(id, req.motivo()));
    }

    @PatchMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')")
    public ResponseEntity<TransferenciaResponse> enviar(@PathVariable Long id,
                                                          @RequestBody TransferenciaEnvioRequest req) {
        return ResponseEntity.ok(transferenciaService.enviar(id, req.cantidadesEnviadas(), 
            new com.inventory.dto.DespachoRequest(req.transportista(), null, null, null, null, null, null, null)));
    }

    @PatchMapping("/{id}/recibir")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')")
    public ResponseEntity<TransferenciaResponse> recibirTransferencia(@PathVariable Long id,
                                                                        @RequestBody com.inventory.dto.RecepcionRequest req) {
        return ResponseEntity.ok(transferenciaService.confirmarRecepcion(id, req));
    }
}
