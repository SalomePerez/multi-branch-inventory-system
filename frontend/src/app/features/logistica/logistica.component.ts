import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
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
export class LogisticaComponent implements OnInit {
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

  constructor(
    private logisticaService: LogisticaService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Escalonamos las llamadas ligeramente para asegurarnos de que la UI se 
    // estabilice y la base de datos concurrente no reciba todos los requests de golpe.
    this.cargar();
    setTimeout(() => this.cargarStats(), 200);
    setTimeout(() => this.cargarReporte(), 400);
    setTimeout(() => this.logisticaService.getLogisticaSucursalReport().subscribe(r => this.reporteSucursal = r), 600);
  }

  cargar(): void {
    this.loading = true;
    this.logisticaService.enviosActivos().subscribe({
      next: data => { this.envios = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar envíos'; this.loading = false; }
    });
  }

  cargarStats(): void {
    this.logisticaService.getStats().subscribe(s => this.stats = s);
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
        this.cargar();
        this.cargarStats();
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
        this.cargar();
        this.cargarStats();
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
  }

  confirmarRecepcionParcial(): void {
    const cantidades = this.recepcionItems.map(i => i.cantidadRecibida);
    this.logisticaService.entregar(this.envioRecepcion.id, cantidades).subscribe({
      next: () => {
        this.mostrarModalRecepcion = false;
        const esParcial = this.recepcionItems.some((i, idx) => cantidades[idx] < i.cantidadEnviada);
        this.success = esParcial ? 'Recepción parcial registrada' : 'Envío recibido y completado';
        this.cargar();
        this.cargarStats();
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
    // Formato YYYY-MM-DDThh:mm para input datetime-local
    const offset = ahora.getTimezoneOffset() * 60000;
    const localISOTime = new Date(ahora.getTime() - offset).toISOString().slice(0, 16);
    this.despachoData.fechaEstimadaLlegada = localISOTime;
    this.despachoData.tiempoTransitoEstimado = horas;
    this.despachoData.tipoRuta = horas <= 8 ? 'URBANA' : 'NACIONAL';
  }
}
