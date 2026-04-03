export type EstadoOrden = 'PENDIENTE' | 'APROBADA' | 'RECIBIDA' | 'CANCELADA';

export interface OrdenCompraRequest {
  sucursalId: number;
  proveedor: string;
  items: { productoId: number; cantidad: number; precioUnitario: number }[];
  observaciones?: string;
  plazoPago?: number;
}

export interface OrdenCompraItem {
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
}

export interface OrdenCompra {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  proveedor: string;
  estado: EstadoOrden;
  total: number;
  creadoPorNombre: string;
  aprobadoPorNombre?: string;
  observaciones?: string;
  plazoPago: number;
  fechaVencimientoPago?: string;
  items: OrdenCompraItem[];
  createdAt: string;
}

export interface CppResponse {
  productoId: number;
  productoNombre: string;
  cpp: number;
  totalUnidades: number;
  totalInvertido: number;
}
