package com.inventory.service;

import com.inventory.entity.ListaPrecio;
import com.inventory.entity.Producto;
import com.inventory.entity.Categoria;
import com.inventory.repository.ListaPrecioRepository;
import com.inventory.repository.CategoriaRepository;
import com.inventory.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListaPrecioService {

    private final ListaPrecioRepository listaPrecioRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public List<ListaPrecio> listarTodas() {
        return listaPrecioRepository.findAll();
    }

    public List<ListaPrecio> listarActivas() {
        return listaPrecioRepository.findByActivaTrue();
    }

    public ListaPrecio obtener(Long id) {
        return listaPrecioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lista/Descuento no encontrado: " + id));
    }

    @Transactional
    public ListaPrecio guardar(ListaPrecio req, List<Long> categoriaIds) {
        if (req.getId() != null) {
            ListaPrecio existente = obtener(req.getId());
            existente.setNombre(req.getNombre());
            existente.setDescripcion(req.getDescripcion());
            existente.setActiva(req.getActiva());
            existente.setPorcentaje(req.getPorcentaje());
            existente.setCondicionCantidadMinima(req.getCondicionCantidadMinima());
            
            if (categoriaIds != null) {
                Set<Categoria> cats = new HashSet<>(categoriaRepository.findAllById(categoriaIds));
                existente.setCategorias(cats);
            } else {
                existente.setCategorias(new HashSet<>());
            }
            return listaPrecioRepository.save(existente);
        } else {
            if (categoriaIds != null && !categoriaIds.isEmpty()) {
                Set<Categoria> cats = new HashSet<>(categoriaRepository.findAllById(categoriaIds));
                req.setCategorias(cats);
            }
            if (req.getPorcentaje() == null) req.setPorcentaje(BigDecimal.ZERO);
            if (req.getCondicionCantidadMinima() == null) req.setCondicionCantidadMinima(1);
            if (req.getActiva() == null) req.setActiva(true);
            return listaPrecioRepository.save(req);
        }
    }

    @Transactional
    public void alternarEstado(Long id) {
        ListaPrecio lista = obtener(id);
        lista.setActiva(!lista.getActiva());
        listaPrecioRepository.save(lista);
    }

    public List<DescuentoDto> listarTodasDto() {
        return listaPrecioRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DescuentoDto> listarActivasDto() {
        return listaPrecioRepository.findByActivaTrue().stream().map(this::toDto).toList();
    }

    @Transactional
    public DescuentoDto crearDesdeDto(DescuentoRequest req) {
        ListaPrecio entity = new ListaPrecio();
        entity.setNombre(req.nombre());
        entity.setDescripcion(req.descripcion());
        entity.setPorcentaje(req.porcentaje());
        entity.setCondicionCantidadMinima(req.condicionCantidadMinima());
        entity.setActiva(req.activa());
        return toDto(guardar(entity, req.categoriaIds()));
    }

    @Transactional
    public DescuentoDto actualizarDesdeDto(Long id, DescuentoRequest req) {
        ListaPrecio entity = new ListaPrecio();
        entity.setId(id);
        entity.setNombre(req.nombre());
        entity.setDescripcion(req.descripcion());
        entity.setPorcentaje(req.porcentaje());
        entity.setCondicionCantidadMinima(req.condicionCantidadMinima());
        entity.setActiva(req.activa());
        return toDto(guardar(entity, req.categoriaIds()));
    }

    private DescuentoDto toDto(ListaPrecio l) {
        List<Long> catIds = l.getCategorias().stream().map(Categoria::getId).toList();
        return new DescuentoDto(l.getId(), l.getNombre(), l.getDescripcion(), l.getPorcentaje(), l.getCondicionCantidadMinima(), l.getActiva(), catIds);
    }

    public record DescuentoRequest(
        String nombre,
        String descripcion,
        BigDecimal porcentaje,
        Integer condicionCantidadMinima,
        Boolean activa,
        List<Long> categoriaIds
    ) {}

    public record DescuentoDto(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal porcentaje,
        Integer condicionCantidadMinima,
        Boolean activa,
        List<Long> categoriaIds
    ) {}

    /**
     * Calcula el precio final aplicando el porcentaje de descuento si cumple las reglas:
     * - El descuento está activo
     * - La cantidad solicitada >= cantidadMínima
     * - El producto pertenece a alguna categoría del descuento (o el descuento no tiene restricción de categorías)
     */
    public BigDecimal resolverPrecioUnitario(Long productoId, Long listaPrecioId, int cantidadSolicitada, BigDecimal precioPorDefecto) {
        if (listaPrecioId == null) return precioPorDefecto;

        ListaPrecio descuento = listaPrecioRepository.findById(listaPrecioId).orElse(null);
        if (descuento == null || !descuento.getActiva()) return precioPorDefecto;

        // Validar cantidad mínima
        if (cantidadSolicitada < descuento.getCondicionCantidadMinima()) {
            return precioPorDefecto;
        }

        // Validar categoría si el descuento tiene restricciones
        if (descuento.getCategorias() != null && !descuento.getCategorias().isEmpty()) {
            Producto producto = productoRepository.findById(productoId).orElse(null);
            if (producto == null) return precioPorDefecto;

            boolean aplica = descuento.getCategorias().stream()
                    .anyMatch(c -> c.getId().equals(producto.getCategoria().getId()));
            
            if (!aplica) return precioPorDefecto;
        }

        // Aplicar porcentaje (ej: 10% -> 0.10)
        BigDecimal porcentajeEnDecimal = descuento.getPorcentaje()
                .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        
        BigDecimal multiplicador = BigDecimal.ONE.subtract(porcentajeEnDecimal);
        
        return precioPorDefecto.multiply(multiplicador)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
