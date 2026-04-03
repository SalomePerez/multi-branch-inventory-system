import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { UsuarioService, UsuarioResponse } from '../../core/services/usuario.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { ProductoService } from '../../core/services/producto.service';
import { ListaPrecioService, DescuentoDto } from '../../core/services/lista-precio.service';
import { Sucursal } from '../../core/models/sucursal.model';
import { Producto } from '../../core/models/producto.model';
export interface Categoria {
  id: number;
  nombre: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {

  tab: 'usuarios' | 'sucursales' | 'precios' = 'usuarios';

  // Usuarios
  usuarios: UsuarioResponse[] = [];
  mostrarUsuarioForm = false;
  usuarioForm!: FormGroup;
  usuarioAEditar: any = null;

  // Sucursales
  sucursales: Sucursal[] = [];
  mostrarSucursalForm = false;
  sucursalForm!: FormGroup;
  sucursalAEditar: any = null;

  // Gestión de Descuentos
  descuentos: DescuentoDto[] = [];
  mostrarDescuentoForm = false;
  descuentoForm!: FormGroup;
  descuentoAEditar: DescuentoDto | null = null;
  categorias: Categoria[] = [];

  // Deactivación General
  mostrarModalMotivo = false;
  motivoDesactivacion = '';
  tipoADesactivar: 'usuario' | 'sucursal' | null = null;
  idADesactivar: number | null = null;
  nombreADesactivar = '';

  loading = false;
  error = '';
  success = '';

  readonly roles = ['ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO'];

  constructor(
    private usuarioService: UsuarioService,
    private sucursalService: SucursalService,
    private productoService: ProductoService,
    private listaPrecioService: ListaPrecioService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.usuarioForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      rol: ['OPERADOR_INVENTARIO', Validators.required],
      sucursalId: [null]
    });

    this.sucursalForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      direccion: ['', [Validators.required, Validators.maxLength(255)]],
      telefono: [''],
      email: ['', Validators.email]
    });

    this.descuentoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: [''],
      porcentaje: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      condicionCantidadMinima: [1, [Validators.required, Validators.min(1)]],
      activa: [true],
      categoriaIds: [[]]
    });

    this.cargarUsuarios();
    this.cargarSucursales();
    this.cargarCategorias();
  }

  cargarCategorias() {
    this.productoService.listarCategorias().subscribe({
      next: (c: any) => this.categorias = c,
      error: () => this.error = 'Error al cargar categorías'
    });
  }

  // --- Usuarios ---

  cargarUsuarios() {
    this.usuarioService.listar().subscribe({
      next: d => this.usuarios = d,
      error: () => this.error = 'Error al cargar usuarios'
    });
  }

  abrirUsuarioForm(u?: any) {
    this.error = '';
    this.usuarioAEditar = u || null;
    if (u) {
      this.usuarioForm.patchValue({
        nombre: u.nombre, email: u.email, password: '',
        rol: u.rol, sucursalId: u.sucursalId
      });
      this.usuarioForm.get('password')?.clearValidators();
      this.usuarioForm.get('password')?.setValidators([Validators.minLength(8)]);
    } else {
      this.usuarioForm.reset({ rol: 'OPERADOR_INVENTARIO' });
      this.usuarioForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    }
    this.usuarioForm.get('password')?.updateValueAndValidity();
    this.mostrarUsuarioForm = true;
  }

  guardarUsuario() {
    if (this.usuarioForm.invalid) return;
    this.loading = true;
    const req = this.usuarioForm.value;
    
    if (this.usuarioAEditar) {
      this.usuarioService.actualizar(this.usuarioAEditar.id, req).subscribe({
        next: () => {
          this.mostrarUsuarioForm = false;
          this.success = 'Usuario actualizado correctamente';
          this.cargarUsuarios();
          setTimeout(() => this.success = '', 3000);
          this.loading = false;
        },
        error: err => { this.error = err.error?.detail || 'Error al actualizar usuario'; this.loading = false; }
      });
    } else {
      this.usuarioService.crear(req).subscribe({
        next: () => {
          this.mostrarUsuarioForm = false;
          this.success = 'Usuario creado correctamente';
          this.cargarUsuarios();
          setTimeout(() => this.success = '', 3000);
          this.loading = false;
        },
        error: err => { this.error = err.error?.detail || 'Error al crear usuario'; this.loading = false; }
      });
    }
  }

  desactivarUsuario(id: number, nombre: string) {
    this.idADesactivar = id;
    this.nombreADesactivar = nombre;
    this.tipoADesactivar = 'usuario';
    this.motivoDesactivacion = '';
    this.mostrarModalMotivo = true;
  }

  confirmarDesactivacion() {
    if (!this.idADesactivar || !this.tipoADesactivar || !this.motivoDesactivacion.trim()) return;
    
    const obs: import('rxjs').Observable<any> = this.tipoADesactivar === 'usuario' 
      ? this.usuarioService.desactivar(this.idADesactivar, this.motivoDesactivacion)
      : this.sucursalService.desactivar(this.idADesactivar, this.motivoDesactivacion);

    obs.subscribe({
      next: () => {
        this.mostrarModalMotivo = false;
        this.tipoADesactivar === 'usuario' ? this.cargarUsuarios() : this.cargarSucursales();
        this.success = `${this.tipoADesactivar === 'usuario' ? 'Usuario' : 'Sucursal'} desactivado correctamente`;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err: any) => this.error = err.error?.detail || 'Error al desactivar'
    });
  }

  // --- Sucursales ---

  cargarSucursales() {
    this.sucursalService.listar().subscribe({ next: d => this.sucursales = d });
  }

  abrirSucursalForm(s?: any) {
    this.error = '';
    this.sucursalAEditar = s || null;
    if (s) {
      this.sucursalForm.patchValue({
        nombre: s.nombre, direccion: s.direccion,
        telefono: s.telefono, email: s.email
      });
    } else {
      this.sucursalForm.reset();
    }
    this.mostrarSucursalForm = true;
  }

  guardarSucursal() {
    if (this.sucursalForm.invalid) return;
    this.loading = true;
    const req = this.sucursalForm.value;

    if (this.sucursalAEditar) {
      this.sucursalService.actualizar(this.sucursalAEditar.id, req).subscribe({
        next: () => {
          this.mostrarSucursalForm = false;
          this.success = 'Sucursal actualizada';
          this.cargarSucursales();
          setTimeout(() => this.success = '', 3000);
          this.loading = false;
        },
        error: err => { this.error = err.error?.detail || 'Error al actualizar sucursal'; this.loading = false; }
      });
    } else {
      this.sucursalService.crear(req).subscribe({
        next: () => {
          this.mostrarSucursalForm = false;
          this.success = 'Sucursal creada';
          this.cargarSucursales();
          setTimeout(() => this.success = '', 3000);
          this.loading = false;
        },
        error: err => { this.error = err.error?.detail || 'Error al crear sucursal'; this.loading = false; }
      });
    }
  }

  desactivarSucursal(id: number, nombre: string) {
    this.idADesactivar = id;
    this.nombreADesactivar = nombre;
    this.tipoADesactivar = 'sucursal';
    this.motivoDesactivacion = '';
    this.mostrarModalMotivo = true;
  }

  getRolBadge(rol: string): string {
    const map: Record<string, string> = {
      ADMINISTRADOR: 'badge-danger',
      GERENTE_SUCURSAL: 'badge-warning',
      OPERADOR_INVENTARIO: 'badge-info'
    };
    return map[rol] ?? 'badge-default';
  }

  getRolLabel(rol: string): string {
    const map: Record<string, string> = {
      ADMINISTRADOR: 'Admin',
      GERENTE_SUCURSAL: 'Gerente',
      OPERADOR_INVENTARIO: 'Operador'
    };
    return map[rol] ?? rol;
  }

  // --- Gestión de Descuentos ---

  seleccionarTabPrecios() {
    this.tab = 'precios';
    this.cargarDescuentos();
  }

  cargarDescuentos() {
    this.listaPrecioService.todas().subscribe(d => this.descuentos = d);
  }

  abrirDescuentoForm(d?: DescuentoDto) {
    this.error = '';
    this.descuentoAEditar = d || null;
    if (d) {
      this.descuentoForm.patchValue({
        nombre: d.nombre,
        descripcion: d.descripcion,
        porcentaje: d.porcentaje,
        condicionCantidadMinima: d.condicionCantidadMinima,
        activa: d.activa,
        categoriaIds: d.categoriaIds
      });
    } else {
      this.descuentoForm.reset({ porcentaje: 0, condicionCantidadMinima: 1, activa: true, categoriaIds: [] });
    }
    this.mostrarDescuentoForm = true;
  }

  guardarDescuento() {
    if (this.descuentoForm.invalid) return;
    this.loading = true;
    const req = this.descuentoForm.value;

    const op = this.descuentoAEditar 
      ? this.listaPrecioService.actualizar(this.descuentoAEditar.id, req)
      : this.listaPrecioService.crear(req);

    op.subscribe({
      next: () => {
        this.mostrarDescuentoForm = false;
        this.success = 'Descuento guardado correctamente';
        this.cargarDescuentos();
        setTimeout(() => this.success = '', 3000);
        this.loading = false;
      },
      error: err => { this.error = err.error?.detail || 'Error al guardar descuento'; this.loading = false; }
    });
  }

  alternarEstadoDescuento(id: number) {
    this.listaPrecioService.alternarEstado(id).subscribe(() => this.cargarDescuentos());
  }
}
