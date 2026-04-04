import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { LucideAngularModule } from 'lucide-angular';
import { VentaService } from '../../core/services/venta.service';
import { ProductoService } from '../../core/services/producto.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { InventarioService } from '../../core/services/inventario.service';
import { Venta } from '../../core/models/venta.model';
import { Producto } from '../../core/models/producto.model';
import { Sucursal } from '../../core/models/sucursal.model';
import { ListaPrecioService, DescuentoDto } from '../../core/services/lista-precio.service';
@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './ventas.component.html',
  styleUrl: './ventas.component.scss'
})
export class VentasComponent implements OnInit {
  ventas: Venta[] = [];
  productos: Producto[] = [];
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  verTodas = false;
  inventarioSucursal: Map<number, number> = new Map(); // productoId -> cantidad
  loading = false;
  error = '';
  success = '';
  mostrarFormulario = false;
  ventaDetalle: Venta | null = null;
  form!: FormGroup;
  totalVenta$!: Observable<number>;

  descuentos: DescuentoDto[] = [];
  descuentoSeleccionado: DescuentoDto | null = null;

  constructor(
    private ventaService: VentaService,
    private productoService: ProductoService,
    private sucursalService: SucursalService,
    private inventarioService: InventarioService,
    private listaPrecioService: ListaPrecioService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      sucursalId: [null, Validators.required],
      listaPrecioId: [null],
      descuentoGlobal: [0, Validators.min(0)],
      observaciones: [''],
      items: this.fb.array([])
    });

    this.totalVenta$ = this.form.valueChanges.pipe(
      startWith(this.form.value),
      map(value => {
        let sub = 0;
        if (value.items) {
          value.items.forEach((item: any) => {
            if (item.productoId && item.cantidad) {
              const precioBase = this.getPrecio(item.productoId, item.cantidad);
              sub += Math.max(0, precioBase * item.cantidad);
            }
          });
        }
        const descGlob = value.descuentoGlobal || 0;
        return Math.max(0, sub - descGlob);
      })
    );

    this.form.get('listaPrecioId')?.valueChanges.subscribe(id => {
      this.descuentoSeleccionado = this.descuentos.find(d => d.id === +id) || null;
      this.form.updateValueAndValidity(); // trigger total recalc
    });

    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      
      // Si no hay sucursal seleccionada, priorizar la del usuario o la primera de la lista
      if (!this.sucursalSeleccionada && !this.verTodas) {
        this.sucursalSeleccionada = user?.sucursalId ?? (s[0]?.id ?? null);
      }
      this.cargar();
    });
    this.productoService.listar().subscribe(p => this.productos = p);
    this.listaPrecioService.activas().subscribe(lp => {
      this.descuentos = lp;
    });
  }

  get itemsArray(): FormArray { return this.form.get('items') as FormArray; }

  cargar() {
    this.loading = true;
    
    // Si sucursalSeleccionada es 0, significa "Ver Todas"
    const verTodoEfectivo = this.verTodas || this.sucursalSeleccionada === 0;

    const obs = verTodoEfectivo
      ? this.ventaService.listarTodas()
      : this.sucursalSeleccionada
        ? this.ventaService.listarPorSucursal(this.sucursalSeleccionada)
        : null;

    if (!obs) { this.loading = false; return; }
    obs.subscribe({
      next: (data) => { this.ventas = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar ventas'; this.loading = false; }
    });

    if (this.sucursalSeleccionada && !verTodoEfectivo) {
      this.inventarioService.listarPorSucursal(this.sucursalSeleccionada).subscribe((items: any[]) => {
        this.inventarioSucursal.clear();
        items.forEach((i: any) => this.inventarioSucursal.set(i.productoId, i.cantidad));
      });
    }
  }

  toggleVerTodas() {
    this.verTodas = !this.verTodas;
    if (this.verTodas) {
      this.sucursalSeleccionada = 0;
    } else {
      const user = this.authService.user();
      this.sucursalSeleccionada = user?.sucursalId || (this.sucursales[0]?.id || null);
    }
    this.cargar();
  }

  cambiarSucursal(event: Event) {
    const val = +(event.target as HTMLSelectElement).value;
    this.sucursalSeleccionada = val;
    this.verTodas = (val === 0);
    this.cargar();
  }

  get isGerenteOrAdmin(): boolean {
    const rol = this.authService.user()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'GERENTE_SUCURSAL';
  }

  abrirFormulario() {
    this.mostrarFormulario = true;
    this.error = '';
    this.form.patchValue({ sucursalId: this.sucursalSeleccionada, observaciones: '' });
    while (this.itemsArray.length) this.itemsArray.removeAt(0);
    this.agregarItem();
  }

  agregarItem() {
    this.itemsArray.push(this.fb.group({
      productoId: [null, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1)]]
    }));
  }

  quitarItem(i: number) { if (this.itemsArray.length > 1) this.itemsArray.removeAt(i); }

  getPrecioBase(productoId: any): number {
    return this.productos.find(p => p.id === +productoId)?.precioVenta ?? 0;
  }

  getPrecio(productoId: any, cantidad: number = 1): number {
    const id = +productoId;
    const prod = this.productos.find(p => p.id === id);
    if (!prod) return 0;
    const precioBase = prod.precioVenta;

    const desc = this.descuentoSeleccionado;
    if (!desc) return precioBase;

    // Evaluacion dinámica de las condiciones
    if (cantidad < desc.condicionCantidadMinima) return precioBase;
    
    if (desc.categoriaIds && desc.categoriaIds.length > 0) {
      if (!desc.categoriaIds.includes(prod.categoriaId)) return precioBase;
    }

    // Calcula precio aplicando porcentaje
    const multiplicador = 1 - (desc.porcentaje / 100);
    return precioBase * multiplicador;
  }

  esDescuentoPorcentajeAplicado(productoId: any, cantidad: number): boolean {
    const id = +productoId;
    const prod = this.productos.find(p => p.id === id);
    if (!prod) return false;

    const desc = this.descuentoSeleccionado;
    if (!desc) return false;

    if (cantidad < desc.condicionCantidadMinima) return false;
    
    if (desc.categoriaIds && desc.categoriaIds.length > 0) {
      if (!desc.categoriaIds.includes(prod.categoriaId)) return false;
    }
    return true; // Cumple todo y se descontó!
  }

  faltanUnidadesParaDescuento(productoId: any, cantidad: number): number | null {
    const id = +productoId;
    const prod = this.productos.find(p => p.id === id);
    if (!prod) return null;

    const desc = this.descuentoSeleccionado;
    if (!desc) return null;

    if (desc.categoriaIds && desc.categoriaIds.length > 0) {
      if (!desc.categoriaIds.includes(prod.categoriaId)) return null; // No aplica la categoría en absoluto
    }

    if (cantidad > 0 && cantidad < desc.condicionCantidadMinima) {
      return desc.condicionCantidadMinima - cantidad; // Le faltan X unidades
    }
    return null;
  }

  getStockDisponible(productoId: any): number {
    return this.inventarioSucursal.get(+productoId) ?? 0;
  }

  calcularSubtotal(item: any): number {
    const cant = item.value.cantidad || 0;
    const precioBase = this.getPrecio(item.value.productoId, cant);
    return Math.max(0, precioBase * cant);
  }

  registrar() {
    if (this.form.invalid) return;
    this.loading = true;
    this.ventaService.registrar(this.form.value).subscribe({
      next: () => {
        this.mostrarFormulario = false;
        this.success = 'Venta registrada';
        this.cargar();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => { this.error = err.error?.detail || 'Error al registrar'; this.loading = false; }
    });
  }

  verDetalle(v: Venta) { this.ventaDetalle = v; }

  imprimirVenta() {
    window.print();
  }
}
