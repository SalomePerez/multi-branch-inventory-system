import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductoService } from '../../core/services/producto.service';
import { Categoria, Producto } from '../../core/models/producto.model';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './productos.component.html',
  styleUrl: './productos.component.scss'
})
export class ProductosComponent implements OnInit {

  productos: Producto[] = [];
  categorias: Categoria[] = [];
  loading = false;
  error = '';
  mostrarFormulario = false;
  mostrarCategorias = false;
  editandoId: number | null = null;
  form!: FormGroup;
  catForm!: FormGroup;
  
  // Deactivación
  mostrarModalMotivo = false;
  motivoDesactivacion = '';
  productoADesactivar: number | null = null;

  constructor(private productoService: ProductoService, private fb: FormBuilder) {}

  ngOnInit() {
    this.initForm();
    this.cargar();
  }

  initForm() {
    this.form = this.fb.group({
      sku: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      descripcion: [''],
      categoriaId: [null, Validators.required],
      precioCosto: [0, [Validators.required, Validators.min(0)]],
      precioVenta: [0, [Validators.required, Validators.min(0)]],
      unidadMedida: ['unidad']
    });

    this.catForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(250)]]
    });
  }

  cargar() {
    this.loading = true;
    this.productoService.listar().subscribe({
      next: (data) => { this.productos = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar productos'; this.loading = false; }
    });
    this.productoService.listarCategorias().subscribe({
      next: (data) => this.categorias = data
    });
  }

  abrirFormulario(producto?: Producto) {
    this.mostrarFormulario = true;
    this.error = '';
    if (producto) {
      this.editandoId = producto.id;
      this.form.patchValue({
        sku: producto.sku,
        nombre: producto.nombre,
        descripcion: producto.descripcion,
        categoriaId: producto.categoriaId,
        precioCosto: producto.precioCosto,
        precioVenta: producto.precioVenta,
        unidadMedida: producto.unidadMedida
      });
    } else {
      this.editandoId = null;
      this.form.reset({ unidadMedida: 'unidad', precioCosto: 0, precioVenta: 0 });
    }
  }

  cerrarFormulario() {
    this.mostrarFormulario = false;
    this.editandoId = null;
    this.form.reset();
  }

  guardar() {
    if (this.form.invalid) return;
    this.loading = true;
    const req = this.form.value;
    const op = this.editandoId
      ? this.productoService.actualizar(this.editandoId, req)
      : this.productoService.crear(req);

    op.subscribe({
      next: () => { this.cerrarFormulario(); this.cargar(); },
      error: (err) => {
        this.error = err.error?.detail || 'Error al guardar producto';
        this.loading = false;
      }
    });
  }

  desactivar(id: number) {
    this.productoADesactivar = id;
    this.motivoDesactivacion = '';
    this.mostrarModalMotivo = true;
  }

  confirmarDesactivacion() {
    if (!this.productoADesactivar || !this.motivoDesactivacion.trim()) return;
    this.productoService.desactivar(this.productoADesactivar, this.motivoDesactivacion).subscribe({
      next: () => {
        this.mostrarModalMotivo = false;
        this.cargar();
      }
    });
  }

  // --- Categorias ---
  abrirCategorias() {
    this.mostrarCategorias = true;
    this.catForm.reset();
  }

  cerrarCategorias() {
    this.mostrarCategorias = false;
    this.catForm.reset();
  }

  guardarCategoria() {
    if (this.catForm.invalid) return;
    this.loading = true;
    this.productoService.crearCategoria(this.catForm.value).subscribe({
      next: (nuevaCat) => {
        this.categorias.push(nuevaCat);
        this.catForm.reset();
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.detail || 'Error al guardar categoría';
        this.loading = false;
      }
    });
  }
}
