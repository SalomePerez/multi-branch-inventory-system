package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto_unidades", uniqueConstraints = @UniqueConstraint(columnNames = {"producto_id", "unidad_medida_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductoUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "factor_conversion", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal factorConversion = BigDecimal.ONE;

    @Column(name = "es_base", nullable = false)
    @Builder.Default
    private Boolean esBase = false;
}
