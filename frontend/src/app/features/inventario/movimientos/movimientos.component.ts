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
  hasta: string = '';
  verTodas = false;

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
    if (this.verTodas) {
      this.loading = true;
      this.error = '';
      this.reporteService.movimientosTodas(this.desde, this.hasta).subscribe({
        next: d => { this.movimientos = d; this.loading = false; },
        error: () => { this.error = 'Error al cargar movimientos'; this.loading = false; }
      });
    } else {
      if (!this.sucursalId) return;
      this.loading = true;
      this.error = '';
      this.reporteService.movimientos(this.sucursalId, this.desde, this.hasta).subscribe({
        next: d => { this.movimientos = d; this.loading = false; },
        error: () => { this.error = 'Error al cargar movimientos'; this.loading = false; }
      });
    }
  }

  getTipoClass(tipo: string): string {
    const map: Record<string, string> = {
      ENTRADA:                'status-success',
      SALIDA:                 'status-danger',
      TRANSFERENCIA_ENTRADA:  'status-info',
      TRANSFERENCIA_SALIDA:   'status-info',
      AJUSTE:                 'status-warning',
      DEVOLUCION:             'status-purple',
    };
    return map[tipo] ?? 'status-default';
  }

  esSalida(tipo: string): boolean {
    return tipo === 'SALIDA' || tipo === 'TRANSFERENCIA_SALIDA';
  }
}
