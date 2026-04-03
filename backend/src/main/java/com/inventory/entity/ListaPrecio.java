package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "listas_precio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ListaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal porcentaje = java.math.BigDecimal.ZERO;

    @Column(name = "condicion_cantidad_minima")
    @Builder.Default
    private Integer condicionCantidadMinima = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lista_precio_categorias",
        joinColumns = @JoinColumn(name = "lista_precio_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    @Builder.Default
    private java.util.Set<Categoria> categorias = new java.util.HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
