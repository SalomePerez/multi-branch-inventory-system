export interface InventarioItem {
  id: number;
  productoId: number;
  productoSku: string;
  productoNombre: string;
  categoriaNombre: string;
  sucursalId: number;
  sucursalNombre: string;
  cantidad: number;
  stockMinimo: number;
  stockMaximo?: number;
  bajoCritico: boolean;
  unidadMedidaNombre?: string;
  unidadMedidaAbreviatura?: string;
  updatedAt: string;
}

export interface AjusteInventarioRequest {
  productoId: number;
  sucursalId: number;
  cantidad: number;
  stockMinimo?: number;
  stockMaximo?: number;
  motivo: string;
  observaciones?: string;
}
