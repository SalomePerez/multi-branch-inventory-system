import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  rol: string;
  sucursalId: number | null;
  sucursalNombre: string | null;
  activo: boolean;
  motivoDesactivacion?: string;
  createdAt: string;
}

export interface UsuarioRequest {
  nombre: string;
  email: string;
  password: string;
  rol: string;
  sucursalId: number | null;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private url = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  listar(): Observable<UsuarioResponse[]> {
    return this.http.get<UsuarioResponse[]>(this.url);
  }

  crear(req: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.url, req);
  }

  actualizar(id: number, req: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.url}/${id}`, req);
  }

  desactivar(id: number, motivo: string): Observable<UsuarioResponse> {
    return this.http.delete<UsuarioResponse>(`${this.url}/${id}?motivo=${encodeURIComponent(motivo)}`);
  }
}
