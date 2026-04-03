export type EstadoTransferencia = 'PENDIENTE' | 'APROBADA' | 'EN_TRANSITO' | 'COMPLETADA' | 'INCOMPLETA' | 'RECHAZADA';

export interface TransferenciaRequest {
  sucursalOrigenId: number;
  sucursalDestinoId: number;
  items: { productoId: number; cantidadSolicitada: number }[];
  observaciones?: string;
}

export interface TransferenciaItem {
  productoId: number;
  productoNombre: string;
  cantidadSolicitada: number;
  cantidadEnviada?: number;
  cantidadRecibida?: number;
}

export interface Transferencia {
  id: number;
  sucursalOrigenId: number;
  sucursalOrigenNombre: string;
  sucursalDestinoId: number;
  sucursalDestinoNombre: string;
  estado: EstadoTransferencia;
  solicitadoPorNombre: string;
  aprobadoPorNombre?: string;
  observaciones?: string;
  motivoRechazo?: string;
  transportista?: string;
  fechaEstimadaLlegada?: string;
  fechaRealLlegada?: string;
  rutaNombre?: string;
  items: TransferenciaItem[];
  createdAt: string;
  updatedAt: string;
}
