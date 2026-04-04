import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ReporteService } from '../../../core/services/reporte.service';
import { SucursalService } from '../../../core/services/sucursal.service';
import { AuthService } from '../../../core/services/auth.service';
import { Sucursal } from '../../../core/models/sucursal.model';

@Component({
  selector: 'app-movimientos',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './movimientos.component.html',
  styleUrl: './movimientos.component.scss'
})
export class MovimientosComponent implements OnInit {
  sucursales: Sucursal[] = [];
  sucursalId: number | null = null;
  desde: string = '';
  thisMonth: string = '';
  hasta: string = '';
  verTodas = false;
  filtroCategoria: 'TODOS' | 'VENTAS' | 'COMPRAS' | 'TRANSFERENCIAS' | 'DEVOLUCIONES' | 'CATALOGO' | 'ACCESOS' | 'AJUSTES' = 'TODOS';

  movimientos: any[] = [];
  loading = false;
  error = '';

  get isGerenteOrAdmin(): boolean {
    const rol = this.authService.user()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'GERENTE_SUCURSAL';
  }

  constructor(
    private reporteService: ReporteService,
    private sucursalService: SucursalService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const hoy = new Date().toISOString().slice(0, 10);
    const inicioMes = new Date(new Date().getFullYear(), new Date().getMonth(), 1)
      .toISOString().slice(0, 10);
    this.desde = inicioMes;
    this.hasta = hoy;

    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      this.sucursalId = user?.sucursalId ?? (s[0]?.id ?? null);
      this.consultar();
    });
  }

  toggleVerTodas(): void {
    this.verTodas = !this.verTodas;
    this.consultar();
  }

  consultar(): void {
    if (!this.desde || !this.hasta) return;
    this.loading = true;
    this.error = '';
    const obs = this.verTodas 
      ? this.reporteService.movimientosTodas(this.desde, this.hasta)
      : this.reporteService.movimientos(this.sucursalId!, this.desde, this.hasta);

    obs.subscribe({
      next: d => { this.movimientos = d; this.loading = false; },
      error: () => { this.error = 'Error al cargar movimientos'; this.loading = false; }
    });
  }

  get movimientosFiltrados(): any[] {
    if (this.filtroCategoria === 'TODOS') return this.movimientos;
    
    return this.movimientos.filter(m => {
      const tipo = m.tipo || '';
      const ref = (m.referenciaTipo || '').toUpperCase();
      const mot = (m.motivo || '').toUpperCase();

      switch (this.filtroCategoria) {
        case 'VENTAS': 
          return tipo === 'SALIDA' && (ref === 'VENTA' || mot.includes('VENTA'));
        case 'COMPRAS': 
          return tipo === 'ENTRADA' && (ref.includes('COMPRA') || mot.includes('COMPRA') || mot.includes('PRODUCTOS'));
        case 'TRANSFERENCIAS': 
          return tipo.includes('TRANSFERENCIA') || ref.includes('TRANSFERENCIA');
        case 'DEVOLUCIONES': 
          return tipo === 'DEVOLUCION' || mot.includes('DEV');
        case 'ACCESOS': 
          return tipo === 'LOGIN';
        case 'CATALOGO': 
          return tipo.startsWith('PRODUCTO_');
        case 'AJUSTES': 
          return tipo === 'AJUSTE' || mot.includes('AJUSTE') || mot.includes('INICIAL');
        default: return true;
      }
    });
  }

  getTipoClass(tipo: string): string {
    const map: Record<string, string> = {
      ENTRADA:                'status-success',
      SALIDA:                 'status-danger',
      TRANSFERENCIA_ENTRADA:  'status-info',
      TRANSFERENCIA_SALIDA:   'status-info',
      AJUSTE:                 'status-warning',
      DEVOLUCION:             'status-purple',
      PRODUCTO_CREADO:        'status-success',
      PRODUCTO_ACTUALIZADO:   'status-info',
      PRODUCTO_ELIMINADO:     'status-danger',
      LOGIN:                  'status-primary',
    };
    return map[tipo] ?? 'status-default';
  }

  getTipoLabel(tipo: string): string {
    const labels: Record<string, string> = {
      ENTRADA: 'Entrada Stock',
      SALIDA: 'Salida Stock',
      TRANSFERENCIA_ENTRADA: 'Recepción Transf.',
      TRANSFERENCIA_SALIDA: 'Envío Transf.',
      AJUSTE: 'Ajuste Manual',
      DEVOLUCION: 'Devolución',
      PRODUCTO_CREADO: 'Catálogo: Nuevo',
      PRODUCTO_ACTUALIZADO: 'Catálogo: Edición',
      PRODUCTO_ELIMINADO: 'Catálogo: Baja',
      LOGIN: 'Acceso: Login'
    };
    return labels[tipo] ?? tipo;
  }

  esEventoGlobal(m: any): boolean {
    return m.tipo === 'LOGIN' || m.tipo.startsWith('PRODUCTO_');
  }

  esSalida(tipo: string): boolean {
    return tipo === 'SALIDA' || tipo === 'TRANSFERENCIA_SALIDA';
  }
}
