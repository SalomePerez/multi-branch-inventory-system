export interface Categoria {
  id: number;
  nombre: string;
  descripcion?: string;
  activa?: boolean;
}

export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  descripcion?: string;
  categoriaId: number;
  categoriaNombre: string;
  precioCosto: number;
  precioVenta: number;
  unidadMedida: string;
  activo: boolean;
  createdAt: string;
}

export interface ProductoRequest {
  sku: string;
  nombre: string;
  descripcion?: string;
  categoriaId: number;
  precioCosto: number;
  precioVenta: number;
  unidadMedida?: string;
}
