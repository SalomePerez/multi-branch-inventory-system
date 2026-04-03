import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sucursal } from '../models/sucursal.model';
import { environment } from '../../../environments/environment';

export interface SucursalRequest {
  nombre: string;
  direccion: string;
  telefono?: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class SucursalService {
  private url = `${environment.apiUrl}/sucursales`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(this.url);
  }

  crear(req: SucursalRequest): Observable<Sucursal> {
    return this.http.post<Sucursal>(this.url, req);
  }

  actualizar(id: number, req: SucursalRequest): Observable<Sucursal> {
    return this.http.put<Sucursal>(`${this.url}/${id}`, req);
  }

  desactivar(id: number, motivo: string): Observable<Sucursal> {
    return this.http.delete<Sucursal>(`${this.url}/${id}?motivo=${encodeURIComponent(motivo)}`);
  }
}
