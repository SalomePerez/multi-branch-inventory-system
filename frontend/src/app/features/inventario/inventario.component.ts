import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
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
export class InventarioComponent implements OnInit {

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

    this.route.queryParams.subscribe(params => {
      this.filtroCritico = params['filtro'] === 'critico';
      if (params['sucursalId']) {
        this.sucursalSeleccionada = +params['sucursalId'];
      }
      this.cargarInventario();
    });

    this.sucursalService.listar().subscribe(s => {
      this.sucursales = s;
      const user = this.authService.user();
      if (!this.sucursalSeleccionada) {
        if (user?.sucursalId) {
          this.sucursalSeleccionada = user.sucursalId;
        } else if (s.length > 0) {
          this.sucursalSeleccionada = s[0].id;
        }
      }
      this.cargarInventario();
    });

    this.productoService.listar().subscribe(p => this.productos = p);
  }

  cargarInventario() {
    this.loading = true;
    const obs = this.verTodo
      ? this.inventarioService.listarTodo()
      : this.sucursalSeleccionada
        ? this.inventarioService.listarPorSucursal(this.sucursalSeleccionada)
        : null;

    if (!obs) { this.loading = false; return; }

    obs.subscribe({
      next: (data) => {
        this.inventario = this.filtroCritico ? data.filter(i => i.bajoCritico) : data;
        this.loading = false;
      },
      error: () => { this.error = 'Error al cargar inventario'; this.loading = false; }
    });
  }

  toggleVerTodo() {
    this.verTodo = !this.verTodo;
    this.filtroCritico = false;
    this.cargarInventario();
  }

  get filteredInventario(): InventarioItem[] {
    if (!this.searchTerm) return this.inventario;
    const term = this.searchTerm.toLowerCase();
    return this.inventario.filter(i => 
      (i.productoSku?.toLowerCase().includes(term)) || 
      (i.productoNombre?.toLowerCase().includes(term))
    );
  }

  cambiarSucursal(event: Event) {
    this.sucursalSeleccionada = +(event.target as HTMLSelectElement).value;
    this.cargarInventario();
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
        this.cargarInventario();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.detail || 'Error al ajustar inventario';
        this.loading = false;
      }
    });
  }

  get isGerenteOrAdmin(): boolean {
    const rol = this.authService.user()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'GERENTE_SUCURSAL';
  }

  getNombreSucursal(): string {
    return this.sucursales.find(s => s.id === this.sucursalSeleccionada)?.nombre ?? '';
  }
}
