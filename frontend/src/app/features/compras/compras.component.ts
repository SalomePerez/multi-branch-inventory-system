import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { CompraService } from '../../core/services/compra.service';
import { ProductoService } from '../../core/services/producto.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { OrdenCompra, CppResponse } from '../../core/models/compra.model';
import { Producto } from '../../core/models/producto.model';
import { Sucursal } from '../../core/models/sucursal.model';

@Component({
  selector: 'app-compras',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LucideAngularModule],
  templateUrl: './compras.component.html',
  styleUrl: './compras.component.scss'
})
export class ComprasComponent implements OnInit {
  ordenes: OrdenCompra[] = [];
  productos: Producto[] = [];
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  verTodas = false;
  loading = false;
  error = '';
  success = '';
  mostrarFormulario = false;
  detalleOrden: OrdenCompra | null = null;
  form!: FormGroup;

  // Plazo de pago opciones
  readonly plazosDisponibles = [
    { label: 'Contado (0 días)', value: 0 },
    { label: '15 días', value: 15 },
    { label: '30 días', value: 30 },
    { label: '45 días', value: 45 },
    { label: '60 días', value: 60 },
    { label: '90 días', value: 90 },
  ];

  // Histórico
  historicoModo: 'proveedor' | 'producto' = 'proveedor';
  historicoProveedor = '';
  historicoProductoId: number | null = null;
  historicoOrdenes: OrdenCompra[] = [];
  historicoLoading = false;
  historicoError = '';

  // CPP
  cppResult: CppResponse | null = null;
  cppLoading = false;
  cppProductoId: number | null = null;

  constructor(
    private compraService: CompraService,
    private productoService: ProductoService,
    private sucursalService: SucursalService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      sucursalId: [null, Validators.required],
      proveedor: ['', Validators.required],
      observaciones: [''],
      plazoPago: [0, Validators.required],
      items: this.fb.array([])
    });
    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      this.sucursalSeleccionada = user?.sucursalId ?? (s[0]?.id ?? null);
      this.cargar();
    });
    this.productoService.listar().subscribe(p => this.productos = p);
  }

  get itemsArray(): FormArray { return this.form.get('items') as FormArray; }

  cargar() {
    this.loading = true;
    const obs = this.verTodas
      ? this.compraService.listarTodas()
      : this.sucursalSeleccionada
        ? this.compraService.listarPorSucursal(this.sucursalSeleccionada)
        : null;

    if (!obs) { this.loading = false; return; }
    obs.subscribe({
      next: d => { this.ordenes = d; this.loading = false; },
      error: () => { this.error = 'Error al cargar'; this.loading = false; }
    });
  }

  toggleVerTodas() {
    this.verTodas = !this.verTodas;
    this.cargar();
  }

  cambiarSucursal(event: Event) {
    this.sucursalSeleccionada = +(event.target as HTMLSelectElement).value;
    this.verTodas = false;
    this.cargar();
  }

  get isGerenteOrAdmin(): boolean {
    const rol = this.authService.user()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'GERENTE_SUCURSAL';
  }

  abrirFormulario() {
    this.mostrarFormulario = true;
    this.error = '';
    this.form.patchValue({ sucursalId: this.sucursalSeleccionada, proveedor: '', observaciones: '', plazoPago: 0 });
    while (this.itemsArray.length) this.itemsArray.removeAt(0);
    this.agregarItem();
  }

  agregarItem() {
    this.itemsArray.push(this.fb.group({
      productoId: [null, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1)]],
      precioUnitario: [0, [Validators.required, Validators.min(0.01)]]
    }));
  }

  quitarItem(i: number) { if (this.itemsArray.length > 1) this.itemsArray.removeAt(i); }

  onProductoChange(i: number, productoId: number | null) {
    if (!productoId) return;
    const producto = this.productos.find(p => p.id === productoId);
    if (producto) {
      this.itemsArray.at(i).patchValue({ precioUnitario: producto.precioCosto });
    }
  }

  calcularTotal(): number {
    return this.itemsArray.controls.reduce((acc, item) => {
      const v = item.value;
      return acc + (v.cantidad || 0) * (v.precioUnitario || 0);
    }, 0);
  }

  crear() {
    if (this.form.invalid) return;
    this.loading = true;
    this.compraService.crear(this.form.value).subscribe({
      next: () => { this.mostrarFormulario = false; this.success = 'Orden creada'; this.cargar(); setTimeout(() => this.success = '', 3000); },
      error: err => { this.error = err.error?.detail || 'Error'; this.loading = false; }
    });
  }

  aprobar(id: number) {
    this.compraService.aprobar(id).subscribe({ next: () => this.cargar() });
  }

  recibir(id: number) {
    this.compraService.recibir(id).subscribe({
      next: () => { this.success = 'Stock actualizado'; this.cargar(); setTimeout(() => this.success = '', 3000); }
    });
  }

  cancelar(id: number) {
    if (!confirm('¿Cancelar esta orden?')) return;
    this.compraService.cancelar(id).subscribe({ next: () => this.cargar() });
  }

  verDetalle(o: OrdenCompra) { this.detalleOrden = o; }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = { PENDIENTE: 'badge-warning', APROBADA: 'badge-info', RECIBIDA: 'badge-success', CANCELADA: 'badge-danger' };
    return map[estado] ?? 'badge-default';
  }

  // ---- Histórico ----

  buscarHistorico() {
    this.historicoLoading = true;
    this.historicoError = '';
    this.historicoOrdenes = [];

    if (this.historicoModo === 'proveedor') {
      if (!this.historicoProveedor.trim()) {
        this.historicoError = 'Ingrese el nombre del proveedor';
        this.historicoLoading = false;
        return;
      }
      this.compraService.historicoPorProveedor(this.historicoProveedor).subscribe({
        next: d => { this.historicoOrdenes = d; this.historicoLoading = false; },
        error: err => { this.historicoError = err.error?.detail || 'Error al buscar'; this.historicoLoading = false; }
      });
    } else {
      if (!this.historicoProductoId) {
        this.historicoError = 'Seleccione un producto';
        this.historicoLoading = false;
        return;
      }
      this.compraService.historicoPorProducto(this.historicoProductoId).subscribe({
        next: d => { this.historicoOrdenes = d; this.historicoLoading = false; },
        error: err => { this.historicoError = err.error?.detail || 'Error al buscar'; this.historicoLoading = false; }
      });
    }
  }

  cambiarModoHistorico(modo: 'proveedor' | 'producto') {
    this.historicoModo = modo;
    this.historicoOrdenes = [];
    this.historicoError = '';
  }

  onHistoricoProductoChange(event: Event) {
    const val = +(event.target as HTMLSelectElement).value;
    this.historicoProductoId = val || null;
  }

  // ---- CPP ----

  onCppProductoChange(event: Event) {
    const val = +(event.target as HTMLSelectElement).value;
    this.cppProductoId = val || null;
    this.cppResult = null;
  }

  buscarCpp() {
    if (!this.cppProductoId) return;
    this.cppLoading = true;
    this.compraService.getCpp(this.cppProductoId).subscribe({
      next: d => { this.cppResult = d; this.cppLoading = false; },
      error: () => { this.cppLoading = false; }
    });
  }
}
