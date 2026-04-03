import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Alerta {
  id: number;
  tipo: string;
  productoNombre?: string;
  sucursalId?: number;
  sucursalNombre?: string;
  mensaje: string;
  leida: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AlertaService {
  private url = `${environment.apiUrl}/alertas`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(this.url);
  }

  contarNoLeidas(): Observable<{ total: number }> {
    return this.http.get<{ total: number }>(`${this.url}/count`);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.patch<void>(`${this.url}/${id}/leer`, {});
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.patch<void>(`${this.url}/leer-todas`, {});
  }
}
