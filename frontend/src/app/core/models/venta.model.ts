export interface VentaRequest {
  sucursalId: number;
  listaPrecioId?: number;
  descuentoGlobal?: number;
  items: { productoId: number; cantidad: number; descuento?: number }[];
  observaciones?: string;
}

export interface VentaItem {
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  descuentoAplicado?: number;
  subtotal: number;
}

export interface Venta {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  vendedorNombre: string;
  listaPrecioId?: number;
  listaPrecioNombre?: string;
  total: number;
  descuentoTotal?: number;
  observaciones?: string;
  items: VentaItem[];
  createdAt: string;
}
