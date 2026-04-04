export interface Alerta {
  id: number;
  tipo: 'STOCK_BAJO' | 'STOCK_ALTO' | string;
  productoId?: number;
  productoNombre?: string;
  sucursalId?: number;
  sucursalNombre?: string;
  mensaje: string;
  leida: boolean;
  createdAt: string;
}
