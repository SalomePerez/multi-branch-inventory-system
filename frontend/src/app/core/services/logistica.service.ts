import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LogisticaService {
  private url = `${environment.apiUrl}/logistica`;

  constructor(private http: HttpClient) {}

  enviosActivos(): Observable<any[]> {
    return this.http.get<any[]>(`${this.url}/envios-activos`);
  }

  despachar(id: number, req: any): Observable<void> {
    return this.http.post<void>(`${this.url}/despachar/${id}`, req);
  }

  rechazar(id: number, motivo: string): Observable<void> {
    return this.http.post<void>(`${this.url}/rechazar/${id}`, { motivo });
  }

  entregar(id: number, cantidades?: number[]): Observable<void> {
    const body = cantidades ? { cantidades } : {};
    return this.http.post<void>(`${this.url}/entregar/${id}`, body);
  }

  getLogisticaSucursalReport(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/reportes/logistica/sucursal`);
  }

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.url}/stats`);
  }

  getLogisticaReport(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/reportes/logistica`);
  }
}
