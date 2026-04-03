export interface Sucursal {
  id: number;
  nombre: string;
  direccion: string;
  telefono?: string;
  email?: string;
  activa: boolean;
  motivoDesactivacion?: string;
}
