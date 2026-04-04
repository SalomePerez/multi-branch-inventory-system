import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { InventarioService } from '../../core/services/inventario.service';
import { AlertaService } from '../../core/services/alerta.service';
import { Alerta } from '../../core/models/alerta.model';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { ProductoService } from '../../core/services/producto.service';
import { LucideAngularModule } from 'lucide-angular';
import { InventarioItem } from '../../core/models/inventario.model';
import { Sucursal } from '../../core/models/sucursal.model';
import { LogisticaService } from '../../core/services/logistica.service';
import { ReporteService } from '../../core/services/reporte.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions, ChartType } from 'chart.js';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule, BaseChartDirective, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  
  // Product analysis
  productos: any[] = [];
  productoSeleccionado: number | null = null;

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
  ventasMensuales: any[] = [];
  totalAcumulado = 0;
  maxVenta = 8000;
  
  // Prediction Data
  predictionData: any[] = [];
  public predictionChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [{
      data: [],
      label: 'Demanda Proyectada',
      borderColor: '#10b981',
      backgroundColor: 'rgba(16, 185, 129, 0.1)',
      borderDash: [5, 5],
      fill: true,
      tension: 0.4,
      pointRadius: 5
    }]
  };
  
  // Chart.js Configuration
  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Ventas Mensuales',
        fill: true,
        tension: 0.4,
        borderColor: '#2563eb',
        backgroundColor: 'rgba(37, 99, 235, 0.1)',
        pointBackgroundColor: '#2563eb',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#2563eb',
        pointRadius: 6,
        pointHoverRadius: 8,
        borderWidth: 3
      }
    ]
  };

  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1e293b',
        titleFont: { size: 14, weight: 'bold' },
        bodyFont: { size: 13 },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (context) => {
            const label = context.parsed.y !== null ? context.parsed.y.toLocaleString() : '';
            const unit = this.productoSeleccionado ? ' Unid.' : ' $';
            return ` Total: ${label}${unit}`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(0,0,0,0.05)' },
        ticks: {
          callback: (value) => this.productoSeleccionado ? value : `$${value}`,
          color: '#64748b',
          font: { size: 11, weight: 'bold' }
        }
      },
      x: {
        grid: { display: false },
        ticks: {
          color: '#64748b',
          font: { size: 11, weight: 'bold' }
        }
      }
    }
  };

  public predictionChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'bottom' },
      tooltip: {
        backgroundColor: '#1e293b',
        callbacks: {
          label: (context) => {
            const label = context.parsed.y !== null ? context.parsed.y.toLocaleString() : '';
            const unit = this.productoSeleccionado ? ' Unid.' : ' $';
            return ` Proyección: ${label}${unit}`;
          }
        }
      }
    },
    scales: {
      y: { 
        beginAtZero: true, 
        ticks: { 
          callback: (value) => this.productoSeleccionado ? value : `$${value}` 
        } 
      },
      x: { grid: { display: false } }
    }
  };

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
        this.sucursalSeleccionada = null; // "Todas las sucursales" por defecto para administrador
      } else {
        this.sucursalSeleccionada = user?.sucursalId ?? null;
      }
      this.cargar();
    });

    this.productoService.listar().subscribe(p => {
      this.productos = p;
      this.totalProductosCatalogo = p.length;
    });

    this.alertaService.listarNoLeidas().subscribe(res => {
      const user = this.authService.user();
      if (user?.rol !== 'ADMINISTRADOR') {
        const alertasSede = res.filter(a => a.sucursalId === user?.sucursalId);
        this.alertas = alertasSede;
        this.totalAlertas = alertasSede.length;
      } else {
        this.alertas = res;
        this.totalAlertas = res.length;
      }
    });

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
      this.cargarGraficas();
    }
  }

  cargar() {
    this.cargarInventario();
    this.cargarGraficas();
  }

  cargarInventario() {
    if (!this.sucursalSeleccionada) {
      this.inventario = []; // O cargar todo si fuera necesario, pero usualmente inventario es por sucursal
      return;
    }
    this.loading = true;
    this.inventarioService.listarPorSucursal(this.sucursalSeleccionada).subscribe({
      next: d => { this.inventario = d; this.loading = false; },
      error: () => this.loading = false
    });
  }

  cargarGraficas() {
    const currentUser = this.authService.user();
    if (currentUser?.rol === 'OPERADOR_INVENTARIO') return;

    this.reporteService.mensual(6, this.sucursalSeleccionada, this.productoSeleccionado).subscribe(data => {
      this.ventasMensuales = [...data].reverse();
      this.totalAcumulado = this.ventasMensuales.reduce((acc, curr) => acc + curr.total, 0);
      this.actualizarGrafico(this.ventasMensuales);
    });

    this.reporteService.prediccion(6, this.sucursalSeleccionada, this.productoSeleccionado).subscribe(data => {
      this.predictionData = data;
      this.actualizarPrediccion(data);
    });

    if (!this.productoSeleccionado) {
       this.reporteService.mensual(2, this.sucursalSeleccionada).subscribe(data => {
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
    } else {
      // Si hay producto, el balance mensual se refiere a las unidades vendidas del producto
      this.comparativaMensual = null; 
    }
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

  actualizarGrafico(data: any[]) {
    const labels = data.map(m => this.formatearMes(m.mes));
    const values = data.map(m => m.total);
    const unitLabel = this.productoSeleccionado ? 'Unidades' : 'Ventas ($)';

    // Resaltar SOLO el punto máximo absoluto
    const maxVal = Math.max(...values);

    this.lineChartData = {
      labels: labels,
      datasets: [{
        ...this.lineChartData.datasets[0],
        label: unitLabel,
        data: values,
        pointRadius: values.map(v => (v === maxVal) ? 10 : 0), // Solo el punto máximo tiene radio visible por defecto
        pointHoverRadius: values.map(v => 10), // Todos crecen al pasar mouse
        pointBackgroundColor: values.map(v => {
          if (v === maxVal) return '#10b981'; // Green for Max Performance
          return '#2563eb';
        }),
        pointBorderWidth: values.map(v => v === maxVal ? 4 : 0)
      }]
    };
  }

  formatearMes(mesIso: string): string {
    const [year, month] = mesIso.split('-');
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${meses[parseInt(month) - 1]} ${year}`;
  }

  actualizarPrediccion(data: any[]) {
    // Combinar últimos 2 meses reales con 2 proyectados para contexto
    const ultimosVentasReales = this.ventasMensuales.slice(-2);
    const labels = [
      ...ultimosVentasReales.map(v => this.formatearMes(v.mes)),
      ...data.map(p => this.formatearMes(p.mes) + ' (Est.)')
    ];
    
    const values = [
      ...ultimosVentasReales.map(v => v.total),
      ...data.map(p => p.total)
    ];

    this.predictionChartData = {
      labels: labels,
      datasets: [
        {
          label: 'Ventas Reales',
          data: [...ultimosVentasReales.map(v => v.total), ...new Array(data.length).fill(null)],
          borderColor: '#2563eb',
          tension: 0.4,
          pointRadius: 6
        },
        {
          label: 'Predicción de Demanda',
          data: [...new Array(ultimosVentasReales.length - 1).fill(null), ultimosVentasReales[ultimosVentasReales.length-1].total, ...data.map(p => p.total)],
          borderColor: '#10b981',
          borderDash: [5, 5],
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 6
        }
      ]
    };
  }

  verStockCritico() {
    let queryParams: any = { filtro: 'critico' };
    if (this.stockCriticoTodas.length > 0) {
      queryParams.sucursalId = this.stockCriticoTodas[0].sucursalId;
    }
    this.router.navigate(['/inventario'], { queryParams });
  }
}
