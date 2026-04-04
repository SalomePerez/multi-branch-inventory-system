import { Component, OnInit, OnDestroy, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { forkJoin, Subject, takeUntil, switchMap, startWith } from 'rxjs';
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
  
  // Signal para reactividad en cambios de sucursal
  private sucursalSeleccionada$$ = signal<number | null>(null);
  
  inventario: InventarioItem[] = [];
  searchTerm = '';
  productos: Producto[] = [];
  sucursales: Sucursal[] = [];
  sucursalSeleccionada: number | null = null;
  verTodo = false;
  loading = true;
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
  ) {
    // Effect para reaccionar a cambios reactivos de sucursal
    effect(() => {
      const sucursalId = this.sucursalSeleccionada$$();
      if (sucursalId !== null && this.sucursales.length > 0) {
        this.cargarInventarioPara(sucursalId);
      }
    });
  }

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

    // Parse query params una sola vez al inicializar
    const params = this.route.snapshot.queryParams;
    this.filtroCritico = params['filtro'] === 'critico';

    this.loading = true;
    this.error = '';

    // Paso 1: Cargar Sucursales y Productos primero (independientes)
    forkJoin([
      this.sucursalService.listar(),
      this.productoService.listar()
    ]).pipe(takeUntil(this.destroy$)).subscribe({
      next: ([sucursales, productos]) => {
        this.sucursales = sucursales;
        this.productos = productos;

        // Paso 2: Después de cargar sucursales, determinar sucursalSeleccionada
        this.determinarSucursalSeleccionada(params);

        // Paso 3: Ahora sí cargar el inventario con sucursalSeleccionada correcta
        this.cargarInventarioPara(this.sucursalSeleccionada ?? 0);
      },
      error: () => {
        this.error = 'Error al cargar sucursales o productos';
        this.loading = false;
      }
    });
  }

  /**
   * Determina la sucursalSeleccionada basado en:
   * 1. Query params (prioridad máxima)
   * 2. Rol del usuario
   * 3. Sucursal asignada al usuario
   * 4. Primera sucursal disponible como fallback
   */
  private determinarSucursalSeleccionada(params: any) {
    const user = this.authService.user();

    if (params['sucursalId']) {
      this.sucursalSeleccionada = +params['sucursalId'];
      this.verTodo = false;
    } else if (user?.rol === 'ADMINISTRADOR') {
      // Los admins pueden ver todas las sucursales
      this.sucursalSeleccionada = user?.sucursalId ?? this.sucursales[0]?.id ?? null;
      this.verTodo = user?.sucursalId === 0 || !user?.sucursalId;
    } else if (user?.sucursalId) {
      // Usuarios no-admin ven solo su sucursal
      this.sucursalSeleccionada = user.sucursalId;
      this.verTodo = false;
    } else if (this.sucursales.length > 0) {
      // Fallback: primera sucursal
      this.sucursalSeleccionada = this.sucursales[0].id;
      this.verTodo = false;
    } else {
      // Sin sucursales disponibles
      this.sucursalSeleccionada = null;
      this.verTodo = true;
    }
  }

  /**
   * Carga el inventario para una sucursal específica o todas.
   * Se utiliza tanto en ngOnInit como en cambios reactivos.
   */
  private cargarInventarioPara(sucursalId: number | null) {
    const verTodoEfectivo = this.verTodo || sucursalId === 0 || sucursalId === null;
    const inventarioObs = verTodoEfectivo
      ? this.inventarioService.listarTodo()
      : sucursalId
      ? this.inventarioService.listarPorSucursal(sucursalId)
      : this.inventarioService.listarTodo();

    this.loading = true;
    this.error = '';

    inventarioObs.pipe(takeUntil(this.destroy$)).subscribe({
      next: (inventarioData) => {
        this.inventario = this.filtroCritico 
          ? inventarioData.filter(i => i.bajoCritico) 
          : inventarioData;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar inventario';
        this.loading = false;
      }
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
    // Trigger reactivo a través del signal
    this.sucursalSeleccionada$$.set(val);
  }

  toggleVerTodo() {
    this.verTodo = !this.verTodo;
    if (this.verTodo) {
      this.sucursalSeleccionada = 0;
      this.sucursalSeleccionada$$.set(0);
    } else {
      const user = this.authService.user();
      this.sucursalSeleccionada = user?.sucursalId || (this.sucursales[0]?.id || null);
      this.sucursalSeleccionada$$.set(this.sucursalSeleccionada ?? 0);
    }
    this.filtroCritico = false;
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
