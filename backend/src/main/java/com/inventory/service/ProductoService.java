package com.inventory.service;

import com.inventory.dto.CategoriaDTO;
import com.inventory.dto.ProductoRequest;
import com.inventory.dto.ProductoResponse;
import com.inventory.entity.*;
import com.inventory.repository.CategoriaRepository;
import com.inventory.repository.ProductoRepository;
import com.inventory.repository.UnidadMedidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final AuditoriaService auditoriaService;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public List<ProductoResponse> listarActivos() {
        return productoRepository.findAllActivosConCategoria()
                .stream().map(this::toResponse).toList();
    }

    public ProductoResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        if (productoRepository.existsBySku(req.sku())) {
            throw new IllegalArgumentException("Ya existe un producto con SKU: " + req.sku());
        }
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + req.categoriaId()));

        UnidadMedida unidad = null;
        if (req.unidadMedidaId() != null) {
            unidad = unidadMedidaRepository.findById(req.unidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada: " + req.unidadMedidaId()));
        }

        Producto producto = Producto.builder()
                .sku(req.sku())
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .categoria(categoria)
                .precioCosto(req.precioCosto())
                .precioVenta(req.precioVenta())
                .unidadMedida(unidad)
                .build();

        Producto productoGuardado = productoRepository.save(producto);
        
        auditoriaService.registrar(com.inventory.entity.enums.TipoMovimiento.PRODUCTO_CREADO, 
                productoGuardado, null, "Creación", "Producto registrado en el catálogo");

        return toResponse(productoGuardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto producto = buscarPorId(id);

        if (!producto.getSku().equals(req.sku()) && productoRepository.existsBySku(req.sku())) {
            throw new IllegalArgumentException("Ya existe un producto con SKU: " + req.sku());
        }

        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + req.categoriaId()));

        StringBuilder cambios = new StringBuilder("Actualización de datos");
        if (producto.getPrecioCosto().compareTo(req.precioCosto()) != 0) {
            cambios.append(String.format(". Costo: $%s -> $%s", producto.getPrecioCosto(), req.precioCosto()));
        }
        if (producto.getPrecioVenta().compareTo(req.precioVenta()) != 0) {
            cambios.append(String.format(". Venta: $%s -> $%s", producto.getPrecioVenta(), req.precioVenta()));
        }

        producto.setSku(req.sku());
        producto.setNombre(req.nombre());
        producto.setDescripcion(req.descripcion());
        producto.setCategoria(categoria);
        producto.setPrecioCosto(req.precioCosto());
        producto.setPrecioVenta(req.precioVenta());
        
        if (req.unidadMedidaId() != null) {
            UnidadMedida unidad = unidadMedidaRepository.findById(req.unidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada: " + req.unidadMedidaId()));
            producto.setUnidadMedida(unidad);
        }

        Producto productoGuardado = productoRepository.save(producto);
        
        auditoriaService.registrar(com.inventory.entity.enums.TipoMovimiento.PRODUCTO_ACTUALIZADO, 
                productoGuardado, null, "Edición", cambios.toString());

        return toResponse(productoGuardado);
    }

    @Transactional
    public void desactivar(Long id, String motivo) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        producto.setMotivoDesactivacion(motivo);
        productoRepository.save(producto);
        
        auditoriaService.registrar(com.inventory.entity.enums.TipoMovimiento.PRODUCTO_ELIMINADO, 
                producto, null, "Eliminación", motivo);
    }

    // --- Unidades de Medida ---

    public List<UnidadMedida> listarUnidades() {
        return unidadMedidaRepository.findAll();
    }

    // --- Categorías ---

    public List<CategoriaDTO> listarCategorias() {
        return categoriaRepository.findByActivaTrue()
                .stream().map(c -> new CategoriaDTO(c.getId(), c.getNombre(), c.getDescripcion(), c.getActiva()))
                .toList();
    }

    @Transactional
    public CategoriaDTO crearCategoria(CategoriaDTO dto) {
        if (categoriaRepository.existsByNombre(dto.nombre())) {
            throw new IllegalArgumentException("Ya existe la categoría: " + dto.nombre());
        }
        Categoria cat = categoriaRepository.save(
                Categoria.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).build()
        );
        return new CategoriaDTO(cat.getId(), cat.getNombre(), cat.getDescripcion(), cat.getActiva());
    }

    // --- Helpers ---

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getSku(), p.getNombre(), p.getDescripcion(),
                p.getCategoria().getId(), p.getCategoria().getNombre(),
                p.getPrecioCosto(), p.getPrecioVenta(), 
                p.getUnidadMedida() != null ? p.getUnidadMedida().getNombre() : "Unidad",
                p.getUnidadMedida() != null ? p.getUnidadMedida().getAbreviatura() : "und",
                p.getActivo(), p.getCreatedAt()
        );
    }
}
