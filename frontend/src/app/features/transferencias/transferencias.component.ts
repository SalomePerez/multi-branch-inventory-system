import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { TransferenciaService } from '../../core/services/transferencia.service';
import { ProductoService } from '../../core/services/producto.service';
import { SucursalService } from '../../core/services/sucursal.service';
import { InventarioService } from '../../core/services/inventario.service';
import { AuthService } from '../../core/services/auth.service';
import { Transferencia } from '../../core/models/transferencia.model';
import { Producto } from '../../core/models/producto.model';
import { Sucursal } from '../../core/models/sucursal.model';
import { InventarioItem } from '../../core/models/inventario.model';

@Component({
  selector: 'app-transferencias',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LucideAngularModule],
  templateUrl: './transferencias.component.html',
  styleUrl: './transferencias.component.scss'
})
export class TransferenciasComponent implements OnInit {
  transferencias: Transferencia[] = [];
  productos: Producto[] = [];
  sucursales: Sucursal[] = [];
  inventarioPorOrigen: InventarioItem[] = [];
  sucursalSeleccionada: number | null = null;
  verTodas = false;
  loading = false;
  error = '';
  errorModal = '';
  success = '';
  mostrarFormulario = false;
  detalleTransferencia: Transferencia | null = null;

  mostrarModalRechazo = false;
  transferenciaArechazar: Transferencia | null = null;
  motivoRechazo = '';

  form!: FormGroup;

  constructor(
    private transferenciaService: TransferenciaService,
    private productoService: ProductoService,
    private sucursalService: SucursalService,
    private inventarioService: InventarioService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      sucursalOrigenId: [null, Validators.required],
      sucursalDestinoId: [null, Validators.required],
      observaciones: [''],
      items: this.fb.array([])
    });

    this.form.get('sucursalOrigenId')!.valueChanges.subscribe(id => {
      if (id) this.cargarInventarioOrigen(id);
      this.itemsArray.clear();
      this.agregarItem();
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

  cargarInventarioOrigen(sucursalId: number): void {
    this.inventarioService.listarPorSucursal(sucursalId).subscribe({
      next: items => this.inventarioPorOrigen = items.filter(i => i.cantidad > 0)
    });
  }

  get productosDisponiblesEnOrigen(): { producto: Producto; stock: number }[] {
    if (!this.inventarioPorOrigen.length) return [];
    return this.inventarioPorOrigen
      .map(inv => {
        const producto = this.productos.find(p => p.id === inv.productoId);
        return producto ? { producto, stock: inv.cantidad } : null;
      })
      .filter((x): x is { producto: Producto; stock: number } => x !== null);
  }

  cargar() {
    if (this.verTodas) {
      this.loading = true;
      this.transferenciaService.listarTodas().subscribe({
        next: d => { this.transferencias = d; this.loading = false; },
        error: () => { this.error = 'Error al cargar'; this.loading = false; }
      });
    } else {
      if (!this.sucursalSeleccionada) return;
      this.loading = true;
      this.transferenciaService.listarPorSucursal(this.sucursalSeleccionada).subscribe({
        next: d => { this.transferencias = d; this.loading = false; },
        error: () => { this.error = 'Error al cargar'; this.loading = false; }
      });
    }
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

  abrirFormulario() {
    this.mostrarFormulario = true;
    this.error = '';
    this.form.patchValue({ sucursalOrigenId: this.sucursalSeleccionada, sucursalDestinoId: null });
    while (this.itemsArray.length) this.itemsArray.removeAt(0);
    this.agregarItem();
    if (this.sucursalSeleccionada) this.cargarInventarioOrigen(this.sucursalSeleccionada);
  }

  agregarItem() {
    this.itemsArray.push(this.fb.group({
      productoId: [null, Validators.required],
      cantidadSolicitada: [1, [Validators.required, Validators.min(1)]]
    }));
  }

  quitarItem(i: number) { if (this.itemsArray.length > 1) this.itemsArray.removeAt(i); }

  solicitar() {
    if (this.form.invalid) return;
    this.loading = true;
    this.transferenciaService.solicitar(this.form.value).subscribe({
      next: () => { this.mostrarFormulario = false; this.success = 'Transferencia solicitada'; this.cargar(); setTimeout(() => this.success = '', 3000); },
      error: err => { this.error = err.error?.detail || 'Error'; this.loading = false; }
    });
  }

  aprobar(id: number) {
    this.transferenciaService.aprobar(id).subscribe({ next: () => this.cargar() });
  }

  abrirModalRechazo(t: Transferencia) {
    this.transferenciaArechazar = t;
    this.motivoRechazo = '';
    this.mostrarModalRechazo = true;
  }

  confirmarRechazo() {
    if (!this.motivoRechazo.trim() || !this.transferenciaArechazar) return;
    this.transferenciaService.rechazar(this.transferenciaArechazar.id, this.motivoRechazo).subscribe({
      next: () => {
        this.mostrarModalRechazo = false;
        this.detalleTransferencia = null;
        this.success = 'Transferencia rechazada';
        this.cargar();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.errorModal = err.error?.detail || 'No se pudo rechazar la transferencia.';
      }
    });
  }

  enviar(t: Transferencia) {
    const transportista = prompt('Ingrese el nombre del transportista / empresa de logística:');
    if (!transportista) return;

    this.errorModal = '';
    const cantidades = t.items.map(i => i.cantidadSolicitada);
    this.transferenciaService.enviar(t.id, cantidades, transportista).subscribe({
      next: () => {
        this.success = 'Enviada, stock descontado del origen';
        this.cargar();
        this.detalleTransferencia = null;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => this.errorModal = err.error?.detail || 'No se pudo enviar. Verifique stock o permisos.'
    });
  }

  recibir(t: Transferencia) {
    this.errorModal = '';
    const cantidades = t.items.map(i => i.cantidadEnviada ?? 0);
    this.transferenciaService.recibir(t.id, cantidades).subscribe({
      next: () => {
        this.success = 'Recepción confirmada';
        this.cargar();
        this.detalleTransferencia = null;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => this.errorModal = err.error?.detail || 'No se pudo confirmar la recepción.'
    });
  }

  verDetalle(t: Transferencia) {
    this.detalleTransferencia = t;
    this.errorModal = '';
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-warning',
      APROBADA: 'badge-info',
      EN_TRANSITO: 'badge',
      COMPLETADA: 'badge-success',
      INCOMPLETA: 'badge-danger',
      RECHAZADA: 'badge-danger'
    };
    return map[estado] ?? 'badge-default';
  }

  get isGerenteOrAdmin(): boolean {
    const user = this.authService.user();
    return user?.rol === 'ADMINISTRADOR' || user?.rol === 'GERENTE_SUCURSAL';
  }
}
