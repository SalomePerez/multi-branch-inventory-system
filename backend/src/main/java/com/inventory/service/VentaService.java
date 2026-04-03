package com.inventory.service;

import com.inventory.dto.VentaRequest;
import com.inventory.dto.VentaResponse;
import com.inventory.entity.*;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final ListaPrecioRepository listaPrecioRepository;
    // La resolución de precio por lista se delega a ListaPrecioService (Single Responsibility)
    private final ListaPrecioService listaPrecioService;

    public List<VentaResponse> listarTodas() {
        return ventaRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<VentaResponse> listarPorSucursal(Long sucursalId) {
        return ventaRepository.findBySucursalIdOrderByCreatedAtDesc(sucursalId)
                .stream().map(this::toResponse).toList();
    }

    public VentaResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public VentaResponse registrar(VentaRequest req) {
        Sucursal sucursal = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + req.sucursalId()));

        Usuario principal = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario vendedor = usuarioRepository.findById(principal.getId()).orElseThrow();

        ListaPrecio listaPrecio = null;
        if (req.listaPrecioId() != null) {
            listaPrecio = listaPrecioRepository.findById(req.listaPrecioId())
                    .orElseThrow(() -> new IllegalArgumentException("Lista de precios no encontrada: " + req.listaPrecioId()));
        }

        BigDecimal descuentoGlobal = req.descuentoGlobal() != null ? req.descuentoGlobal() : BigDecimal.ZERO;

        Venta venta = Venta.builder()
                .sucursal(sucursal)
                .vendedor(vendedor)
                .listaPrecio(listaPrecio)
                .total(BigDecimal.ZERO)
                .descuentoTotal(BigDecimal.ZERO) // se recalcula tras procesar todos los ítems
                .observaciones(req.observaciones())
                .build();

        BigDecimal totalCalculado = BigDecimal.ZERO;
        List<VentaItem> items = new ArrayList<>();

        for (VentaRequest.ItemVentaRequest itemReq : req.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemReq.productoId()));

            // La lista aplicable es la elegida por el cajero
            ListaPrecio listaAplicable = listaPrecio;

            // Delegamos la resolución al servicio centralizado para garantizar consistencia
            // Él evaluará internamente si el carrito cumple la cantidad mínima (itemReq.cantidad())
            // y si el producto pertenece a la categoría necesaria
            BigDecimal precioFinalConDescuentoLista = listaPrecioService.resolverPrecioUnitario(
                    producto.getId(),
                    listaAplicable != null ? listaAplicable.getId() : null,
                    itemReq.cantidad(),
                    producto.getPrecioVenta()
            );

            // El cajero puede haber añadido un descuento adicional manual en plata (opcional)
            BigDecimal descuentoManual = itemReq.descuento() != null ? itemReq.descuento() : BigDecimal.ZERO;
            if (descuentoManual.compareTo(precioFinalConDescuentoLista) > 0) {
                throw new IllegalArgumentException("El descuento adicional por unidad no puede ser mayor al precio para: " + producto.getNombre());
            }

            BigDecimal precioFinal = precioFinalConDescuentoLista.subtract(descuentoManual);
            
            // Validación de Margen: El sistema no dejará vender por debajo del costo, protegiendo ganancias
            if (precioFinal.compareTo(producto.getPrecioCosto()) < 0) {
                throw new IllegalArgumentException(String.format("Margen NEGATIVO: El precio final de %s ($%s) no puede ser inferior a su costo ($%s). Requiere autorización.",
                        producto.getNombre(), precioFinal, producto.getPrecioCosto()));
            }

            BigDecimal subtotal = precioFinal.multiply(BigDecimal.valueOf(itemReq.cantidad()));

            VentaItem item = VentaItem.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(itemReq.cantidad())
                    .precioUnitario(producto.getPrecioVenta()) // Trazabilidad: registramos el original
                    .descuentoAplicado(producto.getPrecioVenta().subtract(precioFinal)) // Trazabilidad: todo lo descontado
                    .subtotal(subtotal)
                    .build();

            items.add(item);
            totalCalculado = totalCalculado.add(subtotal);
        }

        if (descuentoGlobal.compareTo(totalCalculado) > 0) {
            throw new IllegalArgumentException("El descuento global no puede ser mayor al subtotal de la venta");
        }

        // Trazabilidad: descuentoTotal = suma de descuentos por ítem (por cantidad) + descuento global.
        // Este campo permite conocer el impacto financiero total de las rebajas sin hacer JOIN a venta_items.
        BigDecimal descuentoItems = items.stream()
                .map(i -> i.getDescuentoAplicado().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venta.setItems(items);
        venta.setTotal(totalCalculado.subtract(descuentoGlobal));
        venta.setDescuentoTotal(descuentoItems.add(descuentoGlobal));
        
        Venta ventaGuardada = ventaRepository.save(venta);

        for (VentaItem item : ventaGuardada.getItems()) {
            String logDesc = item.getDescuentoAplicado().compareTo(BigDecimal.ZERO) > 0 
                ? "VENTA (Descuento de " + item.getDescuentoAplicado() + " aplicado por unidad)" 
                : "VENTA";
            inventarioService.descontarStock(
                    item.getProducto(), 
                    sucursal, 
                    item.getCantidad(),
                    "VENTA",
                    ventaGuardada.getId(), 
                    logDesc
            );
        }

        return toResponse(ventaGuardada);
    }

    private Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + id));
    }

    private VentaResponse toResponse(Venta v) {
        List<VentaResponse.ItemVentaResponse> items = v.getItems().stream()
                .map(i -> new VentaResponse.ItemVentaResponse(
                        i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidad(), i.getPrecioUnitario(), 
                        i.getDescuentoAplicado(), i.getSubtotal()))
                .toList();

        return new VentaResponse(
                v.getId(),
                v.getSucursal().getId(), v.getSucursal().getNombre(),
                v.getVendedor().getNombre(),
                v.getListaPrecio() != null ? v.getListaPrecio().getId() : null,
                v.getListaPrecio() != null ? v.getListaPrecio().getNombre() : null,
                v.getTotal(), 
                v.getDescuentoTotal(),
                v.getObservaciones(),
                items, v.getCreatedAt()
        );
    }
}
