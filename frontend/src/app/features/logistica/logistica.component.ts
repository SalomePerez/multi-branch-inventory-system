import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, BehaviorSubject, switchMap, takeUntil } from 'rxjs';
import { LogisticaService } from '../../core/services/logistica.service';
import { AuthService } from '../../core/services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-logistica',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, FormsModule],
  templateUrl: './logistica.component.html',
  styleUrl: './logistica.component.scss'
})
export class LogisticaComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private filtroEstado$ = new BehaviorSubject<string>('');

  envios: any[] = [];
  loading = true;
  error = '';
  errorModal = '';
  success = '';

  stats = {
    enTransito: 0,
    pendientesEnvio: 0
  };

  reporteLogistica: any[] = [];
  reporteSucursal: any[] = [];
  mostrarModalRecepcion = false;
  envioRecepcion: any = null;
  recepcionItems: { productoNombre: string; cantidadEnviada: number; cantidadRecibida: number }[] = [];
  mostrarModalDespacho = false;
  mostrarModalRechazo = false;
  envioSeleccionado: any = null;
  motivoRechazo = '';
  despachoData = {
    transportista: '',
    fechaEstimadaLlegada: '',
    prioridad: 'NORMAL',
    rutaNombre: '',
    costoEnvio: 0,
    tiempoTransitoEstimado: 24,
    tipoRuta: 'ESTANDAR',
    notas: ''
  };

  filtroEstado: string = 'TODOS';
  accionFaltante: string = 'DEVOLUCION';
  notasFaltante: string = '';
  discrepanciaDetectada: boolean = false;

  constructor(
    private logisticaService: LogisticaService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Cargar datos iniciales de reportes y estadísticas primero
    this.cargarReporte();
    this.logisticaService.getLogisticaSucursalReport().subscribe(r => this.reporteSucursal = r);

    // Stream reactivo de envíos: se actualiza ante cambios de filtro,
    // mutaciones (notifyChange) y cada 10s (polling automático del servicio)
    // IMPORTANTE: El BehaviorSubject emite inmediatamente, desencadenando la carga inicial
    this.filtroEstado$.pipe(
      switchMap(estado => this.logisticaService.getEnviosStream(estado)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: data => {
        if (Array.isArray(data)) {
          // Filtra filas que realmente tienen datos (sucursalOrigenNombre existe)
          this.envios = data.filter(e => e && e.sucursalOrigenNombre && e.sucursalOrigenNombre.trim());
        } else {
          this.envios = [];
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar envíos:', err);
        this.error = 'Error al cargar envíos';
        this.loading = false;
      }
    });

    // Stream reactivo de estadísticas
    this.logisticaService.getStatsStream().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: s => this.stats = s,
      error: (err) => console.error('Error al cargar estadísticas:', err)
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cambiarFiltro(nuevoEstado: string): void {
    this.filtroEstado = nuevoEstado;
    this.filtroEstado$.next(nuevoEstado === 'TODOS' ? '' : nuevoEstado);
  }

  cargarReporte(): void {
    this.logisticaService.getLogisticaReport().subscribe(r => this.reporteLogistica = r);
  }

  abrirModalDespacho(envio: any): void {
    this.envioSeleccionado = envio;
    this.despachoData = {
      transportista: '',
      fechaEstimadaLlegada: '',
      prioridad: 'NORMAL',
      rutaNombre: `TRUNK-${envio.sucursalOrigenId}-${envio.sucursalDestinoId}`,
      costoEnvio: 150.0,
      tiempoTransitoEstimado: 24,
      tipoRuta: 'ESTANDAR',
      notas: ''
    };
    this.mostrarModalDespacho = true;
    this.errorModal = '';
  }

  confirmarDespacho(): void {
    if (!this.despachoData.transportista) {
      alert('Por favor ingrese el transportista');
      return;
    }

    this.logisticaService.despachar(this.envioSeleccionado.id, {
      ...this.despachoData,
      fechaEstimadaLlegada: this.despachoData.fechaEstimadaLlegada ? new Date(this.despachoData.fechaEstimadaLlegada).toISOString() : null
    }).subscribe({
      next: () => {
        this.success = 'Envío despachado correctamente';
        this.mostrarModalDespacho = false;
        this.cargarReporte();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => this.errorModal = err.error?.detail || 'No se pudo despachar el envío. Verifique stock o permisos.'
    });
  }

  abrirModalRechazo(envio: any): void {
    this.envioSeleccionado = envio;
    this.motivoRechazo = '';
    this.errorModal = '';
    this.mostrarModalRechazo = true;
  }

  confirmarRechazo(): void {
    if (!this.motivoRechazo.trim() || !this.envioSeleccionado) return;
    this.logisticaService.rechazar(this.envioSeleccionado.id, this.motivoRechazo).subscribe({
      next: () => {
        this.mostrarModalRechazo = false;
        this.success = 'Solicitud rechazada';
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => this.errorModal = err.error?.detail || 'No se pudo rechazar el envío.'
    });
  }

  get esAdminOGerente(): boolean {
    const rol = this.authService.user()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'GERENTE_SUCURSAL';
  }

  get operadorActual(): string {
    return this.authService.user()?.nombre || 'Desconocido';
  }

  abrirModalRecepcion(envio: any): void {
    this.envioRecepcion = envio;
    this.recepcionItems = (envio.items || []).map((i: any) => ({
      productoNombre: i.productoNombre,
      cantidadEnviada: i.cantidadEnviada ?? i.cantidadSolicitada,
      cantidadRecibida: i.cantidadEnviada ?? i.cantidadSolicitada
    }));
    this.errorModal = '';
    this.mostrarModalRecepcion = true;
    this.discrepanciaDetectada = false;
    this.accionFaltante = 'DEVOLUCION';
    this.notasFaltante = '';
  }

  verificarDiscrepancia(): void {
    this.discrepanciaDetectada = this.recepcionItems.some(i => i.cantidadRecibida < i.cantidadEnviada);
  }

  confirmarRecepcionParcial(): void {
    const cantidades = this.recepcionItems.map(i => i.cantidadRecibida);
    const body = {
      cantidades,
      accionFaltante: this.discrepanciaDetectada ? this.accionFaltante : null,
      notasFaltante: this.discrepanciaDetectada ? this.notasFaltante : null
    };

    this.logisticaService.entregar(this.envioRecepcion.id, body).subscribe({
      next: () => {
        this.mostrarModalRecepcion = false;
        this.success = this.discrepanciaDetectada ? 'Recepción parcial registrada con acción: ' + this.accionFaltante : 'Envío recibido y completado';
        this.cargarReporte();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => this.errorModal = err.error?.detail || 'Error al confirmar recepción.'
    });
  }

  getEstadoClass(estado: string): string {
    switch (estado) {
      case 'APROBADA': return 'badge-info';
      case 'EN_TRANSITO': return 'badge-warning';
      case 'COMPLETADA': return 'badge-success';
      case 'INCOMPLETA': return 'badge-danger';
      case 'RECHAZADA': return 'badge-error';
      default: return 'badge-default';
    }
  }

  getEstadoLabel(estado: string): string {
    switch (estado) {
      case 'APROBADA': return 'LISTO PARA ENVIAR';
      case 'EN_TRANSITO': return 'EN TRÁNSITO';
      case 'COMPLETADA': return 'ENTREGADO';
      case 'INCOMPLETA': return 'ENTREGADO (INC)';
      default: return estado;
    }
  }

  isDelayed(e: any): boolean {
    if (!e.fechaRealLlegada || !e.fechaEstimadaLlegada) return false;
    return new Date(e.fechaRealLlegada) > new Date(e.fechaEstimadaLlegada);
  }

  getRutasUnicas(): string[] {
    const rutas = this.envios.map(e => e.rutaNombre).filter(r => !!r);
    return [...new Set(rutas)];
  }

  getComplianceRate(ruta: string): number {
    const enviosRuta = this.envios.filter(e => e.rutaNombre === ruta && e.fechaRealLlegada);
    if (enviosRuta.length === 0) return 0;
    const aTiempo = enviosRuta.filter(e => !this.isDelayed(e)).length;
    return Math.round((aTiempo / enviosRuta.length) * 100);
  }

  getDelayedEnvios(): any[] {
    return this.envios.filter(e => e.estado === 'EN_TRANSITO' && this.isDelayed({
      ...e,
      fechaRealLlegada: new Date()
    }));
  }

  sugerirTiempo(horas: number): void {
    const ahora = new Date();
    ahora.setHours(ahora.getHours() + horas);
    const offset = ahora.getTimezoneOffset() * 60000;
    const localISOTime = new Date(ahora.getTime() - offset).toISOString().slice(0, 16);
    this.despachoData.fechaEstimadaLlegada = localISOTime;
    this.despachoData.tiempoTransitoEstimado = horas;
    this.despachoData.tipoRuta = horas <= 8 ? 'URBANA' : 'NACIONAL';
  }

  getProbabilidadRetraso(ruta: string): number {
    const r = this.reporteLogistica.find(x => x.ruta === ruta);
    if (!r) return 0;
    return 100 - r.tasaCumplimiento;
  }
}
