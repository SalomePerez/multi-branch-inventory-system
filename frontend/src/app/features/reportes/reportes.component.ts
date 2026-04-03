import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ReporteService } from '../../core/services/reporte.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { Sucursal } from '../../core/models/sucursal.model';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.scss'
})
export class ReportesComponent implements OnInit {
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  tab: 'movimientos' | 'ventas' | 'stock' = 'stock';
  loading = false;
  error = '';

  movimientos: any[] = [];
  resumenVentas: any = null;
  stockBajo: any[] = [];

  filtroForm!: FormGroup;

  constructor(
    private reporteService: ReporteService,
    private sucursalService: SucursalService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    const hoy = new Date().toISOString().slice(0, 10);
    const inicioMes = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10);

    this.filtroForm = this.fb.group({
      desde: [inicioMes, Validators.required],
      hasta: [hoy, Validators.required]
    });

    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      this.sucursalSeleccionada = user?.sucursalId ?? (s[0]?.id ?? null);
      this.cargarStock();
    });
  }

  cambiarSucursal(event: Event) {
    this.sucursalSeleccionada = +(event.target as HTMLSelectElement).value;
    this.ejecutar();
  }

  cambiarTab(t: 'movimientos' | 'ventas' | 'stock') {
    this.tab = t;
    this.ejecutar();
  }

  ejecutar() {
    if (this.tab === 'stock') this.cargarStock();
    else if (this.tab === 'movimientos') this.cargarMovimientos();
    else this.cargarVentas();
  }

  cargarStock() {
    this.reporteService.stockBajo().subscribe({
      next: d => {
        if (this.sucursalSeleccionada) {
          this.stockBajo = d.filter((i: any) => i.sucursalId === this.sucursalSeleccionada);
        } else {
          this.stockBajo = d;
        }
      },
      error: () => {}
    });
  }

  cargarMovimientos() {
    if (!this.sucursalSeleccionada || this.filtroForm.invalid) return;
    this.loading = true;
    const { desde, hasta } = this.filtroForm.value;
    this.reporteService.movimientos(this.sucursalSeleccionada, desde, hasta).subscribe({
      next: d => { this.movimientos = d; this.loading = false; },
      error: () => { this.error = 'Error'; this.loading = false; }
    });
  }

  cargarVentas() {
    if (!this.sucursalSeleccionada || this.filtroForm.invalid) return;
    this.loading = true;
    const { desde, hasta } = this.filtroForm.value;
    this.reporteService.resumenVentas(this.sucursalSeleccionada, desde, hasta).subscribe({
      next: d => { this.resumenVentas = d; this.loading = false; },
      error: () => { this.error = 'Error'; this.loading = false; }
    });
  }

  exportarCSV() {
    let rows: string[][] = [];
    let nombre = '';

    if (this.tab === 'stock') {
      nombre = 'stock-bajo';
      rows = [
        ['Producto', 'SKU', 'Sucursal', 'Cantidad', 'Stock Mínimo'],
        ...this.stockBajo.map(i => [i.productoNombre, i.productoSku, i.sucursalNombre, i.cantidad, i.stockMinimo])
      ];
    } else if (this.tab === 'movimientos') {
      nombre = 'movimientos';
      rows = [
        ['Tipo', 'Producto', 'Antes', 'Cantidad', 'Después', 'Referencia', 'Usuario', 'Fecha'],
        ...this.movimientos.map(m => [
          m.tipo, m.productoNombre, m.cantidadAntes,
          (m.tipo.includes('SALIDA') ? '-' : '+') + m.cantidad,
          m.cantidadDespues, `${m.referenciaTipo} #${m.referenciaId}`,
          m.usuarioNombre, new Date(m.createdAt).toLocaleString()
        ])
      ];
    } else if (this.tab === 'ventas' && this.resumenVentas) {
      nombre = 'resumen-ventas';
      rows = [
        ['Período', 'Total Ventas', 'Cantidad de Ventas'],
        [`${this.resumenVentas.desde} al ${this.resumenVentas.hasta}`,
          this.resumenVentas.totalVentas, this.resumenVentas.cantidadVentas]
      ];
    }

    if (rows.length === 0) return;

    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${nombre}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
