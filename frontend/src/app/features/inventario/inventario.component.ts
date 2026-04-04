import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin, Subject, takeUntil } from 'rxjs';
import { InventarioService } from '../../core/services/inventario.service';
import { ProductoService } from '../../core/services/producto.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { AuthService } from '../../core/services/auth.service';
import { InventarioItem } from '../../core/models/inventario.model';
import { Producto } from '../../core/models/producto.model';
import { Sucursal } from '../../core/models/sucursal.model';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './inventario.component.html',
  styleUrl: './inventario.component.scss'
})
export class InventarioComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  inventario: InventarioItem[] = [];
  searchTerm = '';
  productos: Producto[] = [];
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  verTodo = false;
  loading = false;
  error = '';
  success = '';
  filtroCritico = false;
  mostrarAjuste = false;
  ajusteForm!: FormGroup;

  constructor(
    private inventarioService: InventarioService,
    private productoService: ProductoService,
    private sucursalService: SucursalService,
    private authService: AuthService,
    private fb: FormBuilder,
    private route: ActivatedRoute
  ) {}

  get esDevolucion(): boolean {
    return this.ajusteForm?.get('motivo')?.value === 'DEVOLUCION';
  }

  ngOnInit() {
    this.ajusteForm = this.fb.group({
      productoId: [null, Validators.required],
      sucursalId: [null, Validators.required],
      cantidad: [0, [Validators.required, Validators.min(0)]],
      motivo: ['', Validators.required],
      stockMinimo: [0, Validators.min(0)],
      stockMaximo: [null],
      observaciones: ['']
    });

    this.ajusteForm.get('motivo')!.valueChanges.subscribe(motivo => {
      const obsCtrl = this.ajusteForm.get('observaciones')!;
      const cantCtrl = this.ajusteForm.get('cantidad')!;
      if (motivo === 'DEVOLUCION') {
        obsCtrl.setValidators([Validators.required, Validators.minLength(5)]);
        cantCtrl.setValidators([Validators.required, Validators.min(1)]);
      } else {
        obsCtrl.setValidators(null);
        cantCtrl.setValidators([Validators.required, Validators.min(0)]);
      }
      obsCtrl.updateValueAndValidity();
      cantCtrl.updateValueAndValidity();
    });

    // Cargar sucursales y productos en paralelo para evitar waterfalls
    forkJoin([
      this.sucursalService.listar(),
      this.productoService.listar()
    ]).pipe(takeUntil(this.destroy$)).subscribe(([sucursales, productos]) => {
      this.sucursales = sucursales;
      this.productos = productos;

      const params = this.route.snapshot.queryParams;
      this.filtroCritico = params['filtro'] === 'critico';
      const user = this.authService.user();

      if (params['sucursalId']) {
        this.sucursalSeleccionada = +params['sucursalId'];
        this.verTodo = false;
      } else if (this.isAdmin) {
        this.sucursalSeleccionada = user?.sucursalId ?? (sucursales[0]?.id ?? null);
        this.verTodo = false;
      } else if (user?.sucursalId) {
        this.sucursalSeleccionada = user.sucursalId;
        this.verTodo = false;
      } else if (sucursales.length > 0) {
        this.sucursalSeleccionada = sucursales[0].id;
        this.verTodo = false;
      }

      this.cargar();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargar() {
    this.loading = true;
    this.error = '';

    const verTodoEfectivo = this.verTodo || this.sucursalSeleccionada === 0;

    const obs = verTodoEfectivo
      ? this.inventarioService.listarTodo()
      : this.sucursalSeleccionada
        ? this.inventarioService.listarPorSucursal(this.sucursalSeleccionada)
        : null;

    if (!obs) {
      this.loading = false;
      return;
    }

    obs.subscribe({
      next: data => {
        this.inventario = this.filtroCritico ? data.filter(i => i.bajoCritico) : data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar inventario';
        this.loading = false;
      }
    });
  }

  get filteredInventario(): InventarioItem[] {
    const validos = this.inventario.filter(i => !!i.productoId);
    if (!this.searchTerm) return validos;
    const term = this.searchTerm.toLowerCase();
    return validos.filter(i =>
      (i.productoSku?.toLowerCase().includes(term)) ||
      (i.productoNombre?.toLowerCase().includes(term))
    );
  }

  cambiarSucursal(event: Event) {
    const val = +(event.target as HTMLSelectElement).value;
    this.sucursalSeleccionada = val;
    this.verTodo = (val === 0);
    this.cargar();
  }

  toggleVerTodo() {
    this.verTodo = !this.verTodo;
    if (this.verTodo) {
      this.sucursalSeleccionada = 0;
    } else {
      const user = this.authService.user();
      this.sucursalSeleccionada = user?.sucursalId || (this.sucursales[0]?.id || null);
    }
    this.filtroCritico = false;
    this.cargar();
  }

  abrirAjuste(item?: InventarioItem) {
    this.mostrarAjuste = true;
    this.error = '';
    this.success = '';
    if (item) {
      this.ajusteForm.patchValue({
        productoId: item.productoId,
        sucursalId: item.sucursalId,
        cantidad: item.cantidad,
        stockMinimo: item.stockMinimo,
        stockMaximo: item.stockMaximo
      });
    } else {
      this.ajusteForm.reset({ cantidad: 0, stockMinimo: 0, sucursalId: this.sucursalSeleccionada });
    }
  }

  guardarAjuste() {
    if (this.ajusteForm.invalid) return;
    this.loading = true;
    this.inventarioService.ajustar(this.ajusteForm.value).subscribe({
      next: () => {
        this.mostrarAjuste = false;
        this.success = 'Stock actualizado correctamente';
        this.cargar();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.detail || 'Error al ajustar inventario';
        this.loading = false;
      }
    });
  }

  eliminar(item: InventarioItem) {
    if (!confirm(`¿Está seguro de eliminar el registro de inventario para ${item.productoNombre}? Esta acción quedará registrada para auditoría.`)) {
      return;
    }
    this.loading = true;
    this.inventarioService.eliminar(item.id).subscribe({
      next: () => {
        this.success = 'Registro eliminado correctamente';
        this.cargar();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.detail || 'Error al eliminar registro';
        this.loading = false;
      }
    });
  }

  get isAdmin(): boolean {
    return this.authService.user()?.rol === 'ADMINISTRADOR';
  }

  get isGerente(): boolean {
    return this.authService.user()?.rol === 'GERENTE_SUCURSAL';
  }

  get isOperador(): boolean {
    return this.authService.user()?.rol === 'OPERADOR_INVENTARIO';
  }

  get isGerenteOrAdmin(): boolean {
    return this.isAdmin || this.isGerente;
  }

  canEdit(item: InventarioItem): boolean {
    const user = this.authService.user();
    if (this.isAdmin) return true;
    return item.sucursalId === user?.sucursalId;
  }

  getNombreSucursal(): string {
    return this.sucursales.find(s => s.id === this.sucursalSeleccionada)?.nombre ?? '';
  }
}
