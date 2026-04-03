package com.inventory.service;

import com.inventory.dto.AlertaResponse;
import com.inventory.entity.Alerta;
import com.inventory.entity.Producto;
import com.inventory.entity.Sucursal;
import com.inventory.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public List<AlertaResponse> listarNoLeidas() {
        return alertaRepository.findByLeidaFalseOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<AlertaResponse> listarPorSucursal(Long sucursalId) {
        return alertaRepository.findBySucursalIdAndLeidaFalse(sucursalId)
                .stream().map(this::toResponse).toList();
    }

    public long contarNoLeidas() {
        return alertaRepository.countByLeidaFalse();
    }

    @Transactional
    public void marcarLeida(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + id));
        alerta.setLeida(true);
        alertaRepository.save(alerta);
    }

    @Transactional
    public void marcarTodasLeidas() {
        List<Alerta> noLeidas = alertaRepository.findByLeidaFalseOrderByCreatedAtDesc();
        noLeidas.forEach(a -> a.setLeida(true));
        alertaRepository.saveAll(noLeidas);
    }

    @Transactional
    public void resolverAlertasStock(Long productoId, Long sucursalId) {
        List<Alerta> noLeidas = alertaRepository.findBySucursalIdAndLeidaFalse(sucursalId)
                .stream()
                .filter(a -> a.getProducto() != null && a.getProducto().getId().equals(productoId))
                .toList();
        noLeidas.forEach(a -> a.setLeida(true));
        alertaRepository.saveAll(noLeidas);
    }

    @Transactional
    public void crearAlertaStockBajo(Producto producto, Sucursal sucursal, int cantidad, int minimo) {
        String mensaje = String.format(
                "⚠️ STOCK BAJO en %s - %s: %d unidades (mínimo: %d)",
                sucursal.getNombre(), producto.getNombre(), cantidad, minimo
        );
        crearAlerta("STOCK_BAJO", producto, sucursal, mensaje);
        enviarEmailSimulado(sucursal, mensaje);
    }

    @Transactional
    public void crearAlertaStockAlto(Producto producto, Sucursal sucursal, int cantidad, int maximo) {
        String mensaje = String.format(
                "🚀 SOBRE STOCK en %s - %s: %d unidades (máximo: %d)",
                sucursal.getNombre(), producto.getNombre(), cantidad, maximo
        );
        crearAlerta("STOCK_ALTO", producto, sucursal, mensaje);
        enviarEmailSimulado(sucursal, mensaje);
    }

    @Transactional
    public void crearAlerta(String tipo, Producto producto, Sucursal sucursal, String mensaje) {
        Alerta alerta = Alerta.builder()
                .tipo(tipo)
                .producto(producto)
                .sucursal(sucursal)
                .mensaje(mensaje)
                .build();
        alertaRepository.save(alerta);
    }

    private void enviarEmailSimulado(Sucursal sucursal, String mensaje) {
        // Simulación de envío de correo según requerimiento 3.7 (opcional)
        System.out.println("📧 [SIMULACIÓN EMAIL] Enviando alerta a sucursal " + sucursal.getNombre() + ": " + mensaje);
    }

    private AlertaResponse toResponse(Alerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getTipo(),
                a.getProducto() != null ? a.getProducto().getId() : null,
                a.getProducto() != null ? a.getProducto().getNombre() : null,
                a.getSucursal() != null ? a.getSucursal().getId() : null,
                a.getSucursal() != null ? a.getSucursal().getNombre() : null,
                a.getMensaje(),
                a.getLeida(),
                a.getCreatedAt()
        );
    }
}
