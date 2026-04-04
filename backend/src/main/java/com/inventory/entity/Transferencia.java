package com.inventory.entity;

import com.inventory.entity.enums.EstadoTransferencia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transferencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_origen_id", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_destino_id", nullable = false)
    private Sucursal sucursalDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoTransferencia estado = EstadoTransferencia.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitado_por", nullable = false)
    private Usuario solicitadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(length = 100)
    private String transportista;

    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransferenciaItem> items = new ArrayList<>();

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "fecha_estimada_llegada")
    private LocalDateTime fechaEstimadaLlegada;

    @Column(name = "fecha_real_llegada")
    private LocalDateTime fechaRealLlegada;

    @Column(length = 20)
    @Builder.Default
    private String prioridad = "NORMAL";

    @Column(name = "ruta_nombre")
    private String rutaNombre;

    @Column(name = "costo_envio")
    private Double costoEnvio;
    
    @Column(name = "tiempo_transito_estimado")
    private Integer tiempoTransitoEstimado;
    
    @Column(name = "tipo_ruta")
    private String tipoRuta;
    
    @Column(name = "notas_despacho", columnDefinition = "TEXT")
    private String notasDespacho;
    
    @Column(name = "operador_despacho")
    private String operadorDespacho;

    @Column(name = "accion_faltante", length = 50)
    private String accionFaltante;

    @Column(name = "notas_faltante", columnDefinition = "TEXT")
    private String notasFaltante;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
