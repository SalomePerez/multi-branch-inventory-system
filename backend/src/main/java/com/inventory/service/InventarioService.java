package com.inventory.service;

import com.inventory.dto.AjusteInventarioRequest;
import com.inventory.dto.InventarioResponse;
import com.inventory.entity.*;
import com.inventory.entity.enums.TipoMovimiento;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final MovimientoRepository movimientoRepository;
    private final AlertaService alertaService;

    public List<InventarioResponse> listarTodo() {
        return inventarioRepository.findAllConProducto()
                .stream().map(this::toResponse).toList();
    }

    public List<InventarioResponse> listarPorSucursal(Long sucursalId) {
        return inventarioRepository.findBySucursalIdConProducto(sucursalId)
                .stream().map(this::toResponse).toList();
    }

    public List<InventarioResponse> listarStockBajo() {
        return inventarioRepository.findStockBajoMinimo()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventarioResponse ajustar(AjusteInventarioRequest req) {
        Producto producto = productoRepository.findById(req.productoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + req.productoId()));
        Sucursal sucursal = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + req.sucursalId()));

        Inventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(req.productoId(), req.sucursalId())
                .orElse(Inventario.builder().producto(producto).sucursal(sucursal).build());

        // Devolución: cantidad representa unidades devueltas (delta), no el nuevo total
        if ("DEVOLUCION".equals(req.motivo())) {
            if (req.observaciones() == null || req.observaciones().isBlank()) {
                throw new IllegalArgumentException(
                        "El campo 'observaciones' es obligatorio en devoluciones. Indique el número de venta o cliente.");
            }
            if (req.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad devuelta debe ser mayor a 0.");
            }
            int cantidadAntes = inventario.getCantidad();
            inventario.setCantidad(cantidadAntes + req.cantidad());
            if (req.stockMinimo() != null) inventario.setStockMinimo(req.stockMinimo());
            inventarioRepository.save(inventario);

            registrarMovimiento(TipoMovimiento.DEVOLUCION, producto, sucursal,
                    req.cantidad(), cantidadAntes, inventario.getCantidad(),
                    null, "DEVOLUCION", req.motivo(), req.observaciones());

            if (inventario.estaSobreStockMaximo()) {
                alertaService.crearAlertaStockAlto(producto, sucursal, inventario.getCantidad(), inventario.getStockMaximo());
            } else {
                alertaService.resolverAlertasStock(producto.getId(), sucursal.getId());
            }
            return toResponse(inventario);
        }

        // Ajuste estándar: cantidad es el nuevo total absoluto
        int cantidadAntes = inventario.getCantidad();
        int cantidadNueva = req.cantidad();
        int diferencia = cantidadNueva - cantidadAntes;

        inventario.setCantidad(cantidadNueva);
        if (req.stockMinimo() != null) inventario.setStockMinimo(req.stockMinimo());
        if (req.stockMaximo() != null) inventario.setStockMaximo(req.stockMaximo());

        inventarioRepository.save(inventario);

        registrarMovimiento(
                diferencia >= 0 ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA,
                producto, sucursal,
                Math.abs(diferencia), cantidadAntes, cantidadNueva,
                null, "AJUSTE", req.motivo(), req.observaciones()
        );

        if (inventario.estaBajoStockMinimo()) {
            alertaService.crearAlertaStockBajo(producto, sucursal, inventario.getCantidad(), inventario.getStockMinimo());
        } else {
            alertaService.resolverAlertasStock(producto.getId(), sucursal.getId());
        }

        if (inventario.estaSobreStockMaximo()) {
            alertaService.crearAlertaStockAlto(producto, sucursal, inventario.getCantidad(), inventario.getStockMaximo());
        }

        return toResponse(inventario);
    }

    @Transactional
    public void eliminar(Long inventarioId) {
        Inventario inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de inventario no encontrado: " + inventarioId));

        // Registrar movimiento de salida para trazabilidad
        registrarMovimiento(
                TipoMovimiento.SALIDA,
                inventario.getProducto(),
                inventario.getSucursal(),
                inventario.getCantidad(),
                inventario.getCantidad(),
                0, // Después de eliminar, el stock es virtualmente 0
                null,
                "ELIMINACION",
                "ELIMINACION_REGISTRO",
                "Registro de inventario eliminado por el usuario."
        );

        // Resolver alertas antes de borrar
        alertaService.resolverAlertasStock(inventario.getProducto().getId(), inventario.getSucursal().getId());

        inventarioRepository.delete(inventario);
    }

    @Transactional
    public void descontarStock(Producto producto, Sucursal sucursal, int cantidad,
                               String motivo, Long referenciaId, String referenciaTipo) {
        Inventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay inventario de " + producto.getNombre() + " en " + sucursal.getNombre()));

        if (inventario.getCantidad() < cantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente de " + producto.getNombre() +
                    ". Disponible: " + inventario.getCantidad() + ", Requerido: " + cantidad);
        }

        int antes = inventario.getCantidad();
        inventario.setCantidad(antes - cantidad);
        inventarioRepository.save(inventario);

        registrarMovimiento(TipoMovimiento.SALIDA, producto, sucursal,
                cantidad, antes, inventario.getCantidad(), referenciaId, referenciaTipo, motivo, null);

        if (inventario.estaBajoStockMinimo()) {
            alertaService.crearAlertaStockBajo(producto, sucursal, inventario.getCantidad(), inventario.getStockMinimo());
        }
    }

    @Transactional
    public void incrementarStock(Producto producto, Sucursal sucursal, int cantidad,
                                 String motivo, Long referenciaId, String referenciaTipo) {
        Inventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
                .orElse(Inventario.builder().producto(producto).sucursal(sucursal).cantidad(0).build());

        int antes = inventario.getCantidad();
        if (antes < 0) antes = 0; // Sanity check
        inventario.setCantidad(antes + cantidad);
        inventarioRepository.save(inventario);

        registrarMovimiento(TipoMovimiento.ENTRADA, producto, sucursal,
                cantidad, antes, inventario.getCantidad(), referenciaId, referenciaTipo, motivo, null);

        if (inventario.estaSobreStockMaximo()) {
            alertaService.crearAlertaStockAlto(producto, sucursal, inventario.getCantidad(), inventario.getStockMaximo());
        }
        if (!inventario.estaBajoStockMinimo()) {
            alertaService.resolverAlertasStock(producto.getId(), sucursal.getId());
        }
    }

    private void registrarMovimiento(TipoMovimiento tipo, Producto producto, Sucursal sucursal,
                                     int cantidad, int antes, int despues,
                                     Long referenciaId, String referenciaTipo, String motivo, String observaciones) {
        Usuario usuario = obtenerUsuarioActual();
        Movimiento m = Movimiento.builder()
                .tipo(tipo)
                .producto(producto)
                .sucursal(sucursal)
                .cantidad(cantidad)
                .cantidadAntes(antes)
                .cantidadDespues(despues)
                .referenciaId(referenciaId)
                .referenciaTipo(referenciaTipo)
                .usuario(usuario)
                .motivo(motivo)
                .observaciones(observaciones)
                .build();
        movimientoRepository.save(m);
    }

    private Usuario obtenerUsuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Usuario ? (Usuario) principal : null;
    }

    private InventarioResponse toResponse(Inventario i) {
        return new InventarioResponse(
                i.getId(),
                i.getProducto().getId(),
                i.getProducto().getSku(),
                i.getProducto().getNombre(),
                i.getProducto().getCategoria() != null ? i.getProducto().getCategoria().getNombre() : null,
                i.getSucursal().getId(),
                i.getSucursal().getNombre(),
                i.getCantidad(),
                i.getProducto().getUnidadMedida() != null ? i.getProducto().getUnidadMedida().getNombre() : "Unidad",
                i.getProducto().getUnidadMedida() != null ? i.getProducto().getUnidadMedida().getAbreviatura() : "und",
                i.getStockMinimo(),
                i.getStockMaximo(),
                i.estaBajoStockMinimo(),
                i.getUpdatedAt()
        );
    }
}
