package com.inventory.controller;

import com.inventory.dto.TransferenciaResponse;
import com.inventory.entity.Transferencia;
import com.inventory.entity.enums.EstadoTransferencia;
import com.inventory.repository.TransferenciaRepository;
import com.inventory.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistica")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO')")
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class LogisticaController {

    private final TransferenciaService transferenciaService;
    private final TransferenciaRepository transferenciaRepository;

    @GetMapping("/envios-activos")
    public ResponseEntity<List<TransferenciaResponse>> enviosActivos() {
        return ResponseEntity.ok(transferenciaRepository.findAll().stream()
                .filter(t -> t.getEstado() == EstadoTransferencia.APROBADA || 
                            t.getEstado() == EstadoTransferencia.EN_TRANSITO)
                .map(t -> transferenciaService.toResponse(t)) // Usamos el método de mapeo existente (que ahora es público)
                .toList());
    }

    @PostMapping("/despachar/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> despachar(@PathVariable Long id, @RequestBody com.inventory.dto.DespachoRequest req) {
        // En un sistema real vendrían las cantidades, aquí las enviamos todas por simplicidad en este endpoint
        Transferencia t = transferenciaRepository.findById(id).orElseThrow();
        List<Integer> cants = t.getItems().stream().map(i -> i.getCantidadSolicitada()).toList();
        transferenciaService.enviar(id, cants, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/entregar/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> entregar(@PathVariable Long id,
                                          @RequestBody(required = false) com.inventory.dto.RecepcionRequest req) {
        Transferencia t = transferenciaRepository.findById(id).orElseThrow();
        List<Integer> cants;
        if (req != null && req.cantidades() != null && !req.cantidades().isEmpty()) {
            cants = req.cantidades();
        } else {
            cants = t.getItems().stream().map(i -> i.getCantidadEnviada() != null ? i.getCantidadEnviada() : 0).toList();
        }
        transferenciaService.confirmarRecepcion(id, cants);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rechazar/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> rechazar(@PathVariable Long id,
                                          @RequestBody com.inventory.dto.RechazarRequest req) {
        transferenciaService.rechazar(id, req.motivo());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long enTransito = transferenciaRepository.findAll().stream()
                .filter(t -> t.getEstado() == EstadoTransferencia.EN_TRANSITO).count();
        long pendientes = transferenciaRepository.findAll().stream()
                .filter(t -> t.getEstado() == EstadoTransferencia.APROBADA).count();
        
        return ResponseEntity.ok(Map.of(
            "enTransito", enTransito,
            "pendientesEnvio", pendientes
        ));
    }
}
