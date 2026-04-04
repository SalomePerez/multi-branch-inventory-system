package com.inventory.service;

import com.inventory.dto.TransferenciaRequest;
import com.inventory.dto.TransferenciaResponse;
import com.inventory.entity.*;
import com.inventory.entity.enums.EstadoTransferencia;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferenciaService {

    private final TransferenciaRepository transferenciaRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final InventarioService inventarioService;
    private final AlertaService alertaService;

    public List<TransferenciaResponse> listarTodas() {
        return transferenciaRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse).toList();
    }

    public List<TransferenciaResponse> listarPorSucursal(Long sucursalId) {
        return transferenciaRepository.findBySucursal(sucursalId)
                .stream().map(this::toResponse).toList();
    }

    public TransferenciaResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public TransferenciaResponse solicitar(TransferenciaRequest req) {
        if (req.sucursalOrigenId().equals(req.sucursalDestinoId())) {
            throw new IllegalArgumentException("Origen y destino no pueden ser la misma sucursal");
        }

        Sucursal origen = sucursalRepository.findById(req.sucursalOrigenId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal origen no encontrada"));
        Sucursal destino = sucursalRepository.findById(req.sucursalDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal destino no encontrada"));

        Usuario solicitante = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Transferencia transferencia = Transferencia.builder()
                .sucursalOrigen(origen)
                .sucursalDestino(destino)
                .solicitadoPor(solicitante)
                .observaciones(req.observaciones())
                .build();

        Transferencia saved = transferenciaRepository.save(transferencia);

        List<TransferenciaItem> items = new ArrayList<>();
        for (TransferenciaRequest.ItemTransferenciaRequest itemReq : req.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemReq.productoId()));

            items.add(TransferenciaItem.builder()
                    .transferencia(saved)
                    .producto(producto)
                    .cantidadSolicitada(itemReq.cantidadSolicitada())
                    .build());
        }

        saved.setItems(items);
        return toResponse(transferenciaRepository.save(saved));
    }

    @Transactional
    public TransferenciaResponse aprobar(Long id) {
        Transferencia t = buscarPorId(id);
        validarEstado(t, EstadoTransferencia.PENDIENTE, "aprobar");

        Usuario aprobador = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        t.setEstado(EstadoTransferencia.APROBADA);
        t.setAprobadoPor(aprobador);
        return toResponse(transferenciaRepository.save(t));
    }

    @Transactional
    public TransferenciaResponse rechazar(Long id, String motivo) {
        Transferencia t = buscarPorId(id);
        if (t.getEstado() != EstadoTransferencia.PENDIENTE && t.getEstado() != EstadoTransferencia.APROBADA) {
            throw new IllegalArgumentException("No se puede rechazar una transferencia en estado " + t.getEstado());
        }
        t.setEstado(EstadoTransferencia.RECHAZADA);
        t.setMotivoRechazo(motivo);
        return toResponse(transferenciaRepository.save(t));
    }

    /**
     * Al enviar: se descuenta stock del origen y pasa a EN_TRÁNSITO.
     * Se registra la cantidad enviada real (puede diferir de la solicitada).
     */
    @Transactional
    public TransferenciaResponse enviar(Long id, List<Integer> cantidadesEnviadas, com.inventory.dto.DespachoRequest req) {
        Transferencia t = buscarPorId(id);
        validarEstado(t, EstadoTransferencia.APROBADA, "enviar");

        List<TransferenciaItem> items = t.getItems();
        if (cantidadesEnviadas.size() != items.size()) {
            throw new IllegalArgumentException("La cantidad de ítems enviados no coincide");
        }

        for (int i = 0; i < items.size(); i++) {
            TransferenciaItem item = items.get(i);
            int cantEnviada = cantidadesEnviadas.get(i);

            if (cantEnviada < 0 || cantEnviada > item.getCantidadSolicitada()) {
                throw new IllegalArgumentException("Cantidad enviada inválida para: " + item.getProducto().getNombre());
            }

            if (cantEnviada > 0) {
                inventarioService.descontarStock(
                        item.getProducto(), t.getSucursalOrigen(),
                        cantEnviada, "TRANSFERENCIA", t.getId(), "TRANSFERENCIA_SALIDA"
                );
            }
            item.setCantidadEnviada(cantEnviada);
        }

        Usuario despachador = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        t.setEstado(EstadoTransferencia.EN_TRANSITO);
        t.setTransportista(req.transportista());
        t.setFechaSalida(java.time.LocalDateTime.now());
        t.setFechaEstimadaLlegada(req.fechaEstimadaLlegada() != null ? req.fechaEstimadaLlegada() : t.getFechaSalida().plusHours(24));
        t.setPrioridad(req.prioridad() != null ? req.prioridad() : "NORMAL");
        t.setRutaNombre(req.rutaNombre() != null ? req.rutaNombre() : "TRUNK-" + t.getSucursalOrigen().getId() + "-" + t.getSucursalDestino().getId());
        t.setCostoEnvio(req.costoEnvio() != null ? req.costoEnvio() : 0.0);
        
        t.setTiempoTransitoEstimado(req.tiempoTransitoEstimado());
        t.setTipoRuta(req.tipoRuta());
        t.setNotasDespacho(req.notas());
        t.setOperadorDespacho(despachador.getNombre());
        
        return toResponse(transferenciaRepository.save(t));
    }

    /**
     * Al recibir: se acredita stock en destino.
     * Si cantidad recibida < enviada → INCOMPLETA, si igual → COMPLETADA.
     */
    @Transactional
    public TransferenciaResponse confirmarRecepcion(Long id, com.inventory.dto.RecepcionRequest req) {
        Transferencia t = buscarPorId(id);
        validarEstado(t, EstadoTransferencia.EN_TRANSITO, "confirmar recepción");

        List<Integer> cantidadesRecibidas = req.cantidades();

        List<TransferenciaItem> items = t.getItems();
        if (cantidadesRecibidas.size() != items.size()) {
            throw new IllegalArgumentException("La cantidad de ítems recibidos no coincide");
        }

        boolean incompleta = false;

        for (int i = 0; i < items.size(); i++) {
            TransferenciaItem item = items.get(i);
            int cantRecibida = cantidadesRecibidas.get(i);
            int cantEnviada = item.getCantidadEnviada() != null ? item.getCantidadEnviada() : 0;

            if (cantRecibida < 0 || cantRecibida > cantEnviada) {
                throw new IllegalArgumentException("Cantidad recibida inválida para: " + item.getProducto().getNombre());
            }

            if (cantRecibida > 0) {
                inventarioService.incrementarStock(
                        item.getProducto(), t.getSucursalDestino(),
                        cantRecibida, "TRANSFERENCIA", t.getId(), "TRANSFERENCIA_ENTRADA"
                );
            }

            item.setCantidadRecibida(cantRecibida);
            if (cantRecibida < cantEnviada) incompleta = true;
        }

        t.setEstado(incompleta ? EstadoTransferencia.INCOMPLETA : EstadoTransferencia.COMPLETADA);
        t.setFechaRealLlegada(java.time.LocalDateTime.now());

        if (incompleta) {
            t.setAccionFaltante(req.accionFaltante());
            t.setNotasFaltante(req.notasFaltante());

            String msg = String.format("⚠️ FALTANTE detectado en Recepción #%d (Origen: %s). Acción: %s", 
                t.getId(), t.getSucursalOrigen().getNombre(), req.accionFaltante());
            alertaService.crearAlerta("FALTANTE", null, t.getSucursalDestino(), msg);

            // Proceso específico según la acción
            if ("DEVOLUCION".equals(req.accionFaltante())) {
                for (int i = 0; i < items.size(); i++) {
                    TransferenciaItem item = items.get(i);
                    int faltante = (item.getCantidadEnviada() != null ? item.getCantidadEnviada() : 0) - item.getCantidadRecibida();
                    if (faltante > 0) {
                        inventarioService.incrementarStock(
                            item.getProducto(), t.getSucursalOrigen(),
                            faltante, "TRANSFERENCIA_DEVOLUCION", t.getId(), "REINTEGRO_POR_FALTANTE"
                        );
                    }
                }
            } else if ("AJUSTE_INVENTARIO".equals(req.accionFaltante())) {
                for (int i = 0; i < items.size(); i++) {
                    TransferenciaItem item = items.get(i);
                    int faltante = (item.getCantidadEnviada() != null ? item.getCantidadEnviada() : 0) - item.getCantidadRecibida();
                    if (faltante > 0) {
                        // Registramos como MERMA en el origen (donde salió legalmente pero se perdió en el proceso)
                        inventarioService.descontarStock(
                            item.getProducto(), t.getSucursalOrigen(),
                            0, "AJUSTE_LOGISTICO", t.getId(), "PERDIDA_EN_TRANSITO" // 0 porque ya se descontó al salir, solo dejamos rastro si fuera necesario o ajustamos
                        );
                        // En realidad, al enviarse ya se descontó. Si es AJUSTE, simplemente aceptamos que no llegará.
                        // Podríamos registrar un movimiento tipo AJUSTE para trazabilidad.
                    }
                }
            }
        }

        return toResponse(transferenciaRepository.save(t));
    }

    private void validarEstado(Transferencia t, EstadoTransferencia esperado, String accion) {
        if (t.getEstado() != esperado) {
            throw new IllegalArgumentException(
                    "No se puede " + accion + " una transferencia en estado " + t.getEstado());
        }
    }

    private Transferencia buscarPorId(Long id) {
        return transferenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferencia no encontrada: " + id));
    }

    public TransferenciaResponse toResponse(Transferencia t) {
        List<TransferenciaResponse.ItemTransferenciaResponse> items = t.getItems().stream()
                .map(i -> new TransferenciaResponse.ItemTransferenciaResponse(
                        i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidadSolicitada(), i.getCantidadEnviada(), i.getCantidadRecibida()))
                .toList();

        return new TransferenciaResponse(
                t.getId(),
                t.getSucursalOrigen().getId(), t.getSucursalOrigen().getNombre(),
                t.getSucursalDestino().getId(), t.getSucursalDestino().getNombre(),
                t.getEstado().name(),
                t.getSolicitadoPor().getNombre(),
                t.getAprobadoPor() != null ? t.getAprobadoPor().getNombre() : null,
                t.getObservaciones(),
                items,
                t.getFechaSalida(),
                t.getFechaEstimadaLlegada(),
                t.getFechaRealLlegada(),
                t.getPrioridad(),
                t.getRutaNombre(),
                t.getCostoEnvio(),
                t.getTransportista(),
                t.getTiempoTransitoEstimado(),
                t.getTipoRuta(),
                t.getNotasDespacho(),
                t.getOperadorDespacho(),
                t.getCreatedAt(), t.getUpdatedAt(),
                t.getMotivoRechazo(),
                t.getAccionFaltante(),
                t.getNotasFaltante()
        );
    }
}
