import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Alerta } from '../models/alerta.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AlertaService {
  private apiUrl = `${environment.apiUrl}/alertas`;

  constructor(private http: HttpClient) { }

  listarNoLeidas(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(this.apiUrl);
  }

  listarPorSucursal(sucursalId: number): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(`${this.apiUrl}/sucursal/${sucursalId}`);
  }

  contarNoLeidas(): Observable<{ total: number }> {
    return this.http.get<{ total: number }>(`${this.apiUrl}/count`);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/leer`, {});
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/leer-todas`, {});
  }
}
