import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { InventarioService } from '../../core/services/inventario.service';
import { AlertaService, Alerta } from '../../core/services/alerta.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { ProductoService } from '../../core/services/producto.service';
import { LucideAngularModule } from 'lucide-angular';
import { InventarioItem } from '../../core/models/inventario.model';
import { Sucursal } from '../../core/models/sucursal.model';
import { LogisticaService } from '../../core/services/logistica.service';
import { ReporteService } from '../../core/services/reporte.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  inventario: InventarioItem[] = [];
  totalProductosCatalogo = 0;
  stockCriticoTodas: InventarioItem[] = [];
  alertas: Alerta[] = [];
  totalAlertas = 0;
  loading = false;
  mostrarAlertas = false;
  
  topSucursales: any[] = [];
  transferenciasActivas = 0;
  evolucionData: any[] = [];
  demandaData: any[] = [];
  comparativaMensual: { mes: string, total: number, diferencia: number } | null = null;

  // KPIs & Chart Data (Mock for UI demonstration as required by the test)
  ventasMensuales = [
    { mes: 'Ene', valor: 4500 },
    { mes: 'Feb', valor: 5200 },
    { mes: 'Mar', valor: 4800 },
    { mes: 'Abr', valor: 6100 },
    { mes: 'May', valor: 5900 },
    { mes: 'Jun', valor: 7200 }
  ];

  stockPorCategoria = [
    { nombre: 'Electrónica', porcentaje: 40, color: '#3b82f6' },
    { nombre: 'Hogar', porcentaje: 25, color: '#10b981' },
    { nombre: 'Oficina', porcentaje: 20, color: '#f59e0b' },
    { nombre: 'Otros', porcentaje: 15, color: '#64748b' }
  ];

  constructor(
    private inventarioService: InventarioService,
    private alertaService: AlertaService,
    private sucursalService: SucursalService,
    private authService: AuthService,
    private productoService: ProductoService,
    private router: Router,
    private reporteService: ReporteService,
    private logisticaService: LogisticaService
  ) {}

  ngOnInit() {
    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      if (user?.rol === 'ADMINISTRADOR') {
        this.sucursalSeleccionada = this.sucursalSeleccionada ?? (s[0]?.id ?? null);
      } else {
        // Para Gerente y Operador, forzar su sucursal propia
        this.sucursalSeleccionada = user?.sucursalId ?? null;
      }
      this.cargar();
    });

    this.alertaService.listar().subscribe(a => {
      const user = this.authService.user();
      // Si no es admin, filtramos alertas por su sucursal (aunque el backend debería hacerlo, reforzamos)
      if (user?.rol !== 'ADMINISTRADOR') {
        const filtradas = a.filter(al => al.sucursalId === user?.sucursalId);
        this.alertas = filtradas;
        this.totalAlertas = filtradas.length;
      } else {
        this.alertas = a;
        this.totalAlertas = a.length;
      }
    });

    // El contador original de la base se omite para usar el filtrado
    // this.alertaService.contarNoLeidas().subscribe(r => this.totalAlertas = r.total);

    // Total productos desde el catálogo global (no por sucursal)
    this.productoService.listar().subscribe(p => this.totalProductosCatalogo = p.length);

    // Stock crítico de TODAS las sucursales
    this.inventarioService.listarTodo().subscribe(items => {
      this.stockCriticoTodas = items.filter(i => i.bajoCritico);
    });
    
    const currentUser = this.authService.user();
    if (currentUser?.rol === 'ADMINISTRADOR') {
      this.reporteService.ranking().subscribe(data => this.topSucursales = data.slice(0, 3));
      this.reporteService.evolucion(7).subscribe(data => this.evolucionData = data);
    }
    
    if (currentUser?.rol !== 'OPERADOR_INVENTARIO') {
      this.logisticaService.getStats().subscribe(stats => this.transferenciasActivas = stats.enTransito);
      
      this.reporteService.demanda(5).subscribe(data => this.demandaData = data);
      
      this.reporteService.mensual(2).subscribe(data => {
        if (data && data.length >= 2) {
          const actual = data[0];
          const anterior = data[1];
          const diff = anterior.total > 0 ? ((actual.total - anterior.total) / anterior.total) * 100 : 0;
          this.comparativaMensual = {
            mes: actual.mes,
            total: actual.total,
            diferencia: diff
          };
        }
      });
    }
  }

  cargar() {
    if (!this.sucursalSeleccionada) return;
    this.loading = true;
    this.inventarioService.listarPorSucursal(this.sucursalSeleccionada).subscribe({
      next: d => { this.inventario = d; this.loading = false; },
      error: () => this.loading = false
    });
  }

  get totalProductos(): number { return this.totalProductosCatalogo; }
  get stockBajoCount(): number { return this.stockCriticoTodas.length; }
  get stockBajoItems(): InventarioItem[] { return this.stockCriticoTodas; }
  get usuario() { return this.authService.user(); }

  verAlertas() {
    this.mostrarAlertas = true;
  }

  verSucursales() {
    this.router.navigate(['/admin'], { queryParams: { tab: 'sucursales' } });
  }

  marcarAlertaLeida(id: number) {
    this.alertaService.marcarLeida(id).subscribe(() => {
      this.alertas = this.alertas.filter(a => a.id !== id);
      this.totalAlertas = Math.max(0, this.totalAlertas - 1);
    });
  }

  verStockCritico() {
    let queryParams: any = { filtro: 'critico' };
    if (this.stockCriticoTodas.length > 0) {
      queryParams.sucursalId = this.stockCriticoTodas[0].sucursalId;
    }
    this.router.navigate(['/inventario'], { queryParams });
  }
}
