package com.inventory.service;

import com.inventory.dto.CppResponse;
import com.inventory.dto.OrdenCompraRequest;
import com.inventory.dto.OrdenCompraResponse;
import com.inventory.entity.*;
import com.inventory.entity.enums.EstadoOrden;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenCompraService {

    private final OrdenCompraRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final InventarioService inventarioService;
    private final InventarioRepository inventarioRepository;

    public List<OrdenCompraResponse> listarTodas() {
        return ordenRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<OrdenCompraResponse> listarPorSucursal(Long sucursalId) {
        return ordenRepository.findBySucursalIdOrderByCreatedAtDesc(sucursalId)
                .stream().map(this::toResponse).toList();
    }

    public OrdenCompraResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest req) {
        Sucursal sucursal = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + req.sucursalId()));

        Usuario creador = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        OrdenCompra orden = OrdenCompra.builder()
                .sucursal(sucursal)
                .proveedor(req.proveedor())
                .creadoPor(creador)
                .observaciones(req.observaciones())
                .plazoPago(req.plazoPago() != null ? req.plazoPago() : 0)
                .build();

        OrdenCompra savedOrden = ordenRepository.save(orden);

        List<OrdenCompraItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrdenCompraRequest.ItemOrdenRequest itemReq : req.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemReq.productoId()));

            BigDecimal subtotal = itemReq.precioUnitario()
                    .multiply(BigDecimal.valueOf(itemReq.cantidad()));

            OrdenCompraItem item = OrdenCompraItem.builder()
                    .orden(savedOrden)
                    .producto(producto)
                    .cantidad(itemReq.cantidad())
                    .precioUnitario(itemReq.precioUnitario())
                    .build();

            items.add(item);
            total = total.add(subtotal);
        }

        savedOrden.setItems(items);
        savedOrden.setTotal(total);
        return toResponse(ordenRepository.save(savedOrden));
    }

    @Transactional
    public OrdenCompraResponse aprobar(Long id) {
        OrdenCompra orden = buscarPorId(id);

        if (orden.getEstado() != EstadoOrden.PENDIENTE) {
            throw new IllegalArgumentException("Solo se pueden aprobar órdenes en estado PENDIENTE");
        }

        Usuario aprobador = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        orden.setEstado(EstadoOrden.APROBADA);
        orden.setAprobadoPor(aprobador);
        return toResponse(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenCompraResponse recibir(Long id) {
        OrdenCompra orden = buscarPorId(id);

        if (orden.getEstado() != EstadoOrden.APROBADA) {
            throw new IllegalArgumentException("Solo se pueden recibir órdenes en estado APROBADA");
        }

        // Incrementar stock por cada ítem y actualizar precio costo (CPP)
        for (OrdenCompraItem item : orden.getItems()) {
            inventarioService.incrementarStock(
                    item.getProducto(), orden.getSucursal(),
                    item.getCantidad(), "COMPRA", orden.getId(), "ORDEN_COMPRA"
            );

            // Actualizar precioCosto del producto con CPP ponderado
            actualizarPrecioCostoCpp(item.getProducto(), item.getCantidad(), item.getPrecioUnitario());
        }

        orden.setEstado(EstadoOrden.RECIBIDA);
        return toResponse(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenCompraResponse cancelar(Long id) {
        OrdenCompra orden = buscarPorId(id);

        if (orden.getEstado() == EstadoOrden.RECIBIDA) {
            throw new IllegalArgumentException("No se puede cancelar una orden ya recibida");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        return toResponse(ordenRepository.save(orden));
    }

    // ---- Histórico ----

    public List<OrdenCompraResponse> historicoPorProveedor(String nombre) {
        return ordenRepository.findByProveedorContainingIgnoreCase(nombre)
                .stream().map(this::toResponse).toList();
    }

    public List<OrdenCompraResponse> historicoPorProducto(Long productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        return ordenRepository.findByProductoId(productoId)
                .stream().map(this::toResponse).toList();
    }

    // ---- CPP ----

    public CppResponse calcularCpp(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        List<OrdenCompra> ordenesRecibidas = ordenRepository.findByProductoIdAndEstado(productoId, EstadoOrden.RECIBIDA);

        BigDecimal totalUnidades = BigDecimal.ZERO;
        BigDecimal totalInvertido = BigDecimal.ZERO;

        for (OrdenCompra orden : ordenesRecibidas) {
            for (OrdenCompraItem item : orden.getItems()) {
                if (item.getProducto().getId().equals(productoId)) {
                    BigDecimal qty = BigDecimal.valueOf(item.getCantidad());
                    totalUnidades = totalUnidades.add(qty);
                    totalInvertido = totalInvertido.add(item.getPrecioUnitario().multiply(qty));
                }
            }
        }

        BigDecimal cpp = totalUnidades.compareTo(BigDecimal.ZERO) > 0
                ? totalInvertido.divide(totalUnidades, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CppResponse(productoId, producto.getNombre(), cpp, totalUnidades, totalInvertido);
    }

    // ---- Helpers ----

    private void actualizarPrecioCostoCpp(Producto producto, Integer cantidadNueva, BigDecimal precioNuevo) {
        Integer stockActual = inventarioRepository.sumCantidadByProductoId(producto.getId());
        if (stockActual == null) stockActual = 0;

        BigDecimal stockActualBD = BigDecimal.valueOf(stockActual);
        BigDecimal cantidadNuevaBD = BigDecimal.valueOf(cantidadNueva);
        BigDecimal precioActual = producto.getPrecioCosto() != null ? producto.getPrecioCosto() : BigDecimal.ZERO;

        BigDecimal totalAnterior = stockActualBD.multiply(precioActual);
        BigDecimal totalNuevo = cantidadNuevaBD.multiply(precioNuevo);
        BigDecimal totalUnidades = stockActualBD.add(cantidadNuevaBD);

        BigDecimal nuevoCpp = totalUnidades.compareTo(BigDecimal.ZERO) > 0
                ? totalAnterior.add(totalNuevo).divide(totalUnidades, 4, RoundingMode.HALF_UP)
                : precioNuevo;

        producto.setPrecioCosto(nuevoCpp);
        productoRepository.save(producto);
    }

    private OrdenCompra buscarPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden de compra no encontrada: " + id));
    }

    private OrdenCompraResponse toResponse(OrdenCompra o) {
        List<OrdenCompraResponse.ItemOrdenResponse> items = o.getItems().stream()
                .map(i -> new OrdenCompraResponse.ItemOrdenResponse(
                        i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();

        Integer plazoPago = o.getPlazoPago() != null ? o.getPlazoPago() : 0;
        LocalDateTime fechaVencimiento = o.getCreatedAt() != null
                ? o.getCreatedAt().plusDays(plazoPago)
                : null;

        return new OrdenCompraResponse(
                o.getId(),
                o.getSucursal().getId(), o.getSucursal().getNombre(),
                o.getProveedor(),
                o.getEstado().name(),
                o.getTotal(),
                o.getCreadoPor().getNombre(),
                o.getAprobadoPor() != null ? o.getAprobadoPor().getNombre() : null,
                o.getObservaciones(),
                plazoPago,
                fechaVencimiento,
                items,
                o.getCreatedAt()
        );
    }
}
