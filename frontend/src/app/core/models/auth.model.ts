export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  nombre: string;
  rol: 'ADMINISTRADOR' | 'GERENTE_SUCURSAL' | 'OPERADOR_INVENTARIO';
  sucursalId: number | null;
}
