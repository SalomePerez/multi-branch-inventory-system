import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InventarioItem } from '../models/inventario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private url = `${environment.apiUrl}/reportes`;

  constructor(private http: HttpClient) {}

  stockActual(sucursalId: number): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(`${this.url}/stock/${sucursalId}`);
  }

  stockBajo(): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(`${this.url}/stock-bajo`);
  }

  movimientos(sucursalId: number, desde: string, hasta: string): Observable<any[]> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<any[]>(`${this.url}/movimientos/${sucursalId}`, { params });
  }

  movimientosTodas(desde: string, hasta: string): Observable<any[]> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<any[]>(`${this.url}/movimientos`, { params });
  }

  resumenVentas(sucursalId: number, desde: string, hasta: string): Observable<any> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<any>(`${this.url}/ventas/${sucursalId}`, { params });
  }

  resumenVentasTodas(desde: string, hasta: string): Observable<any> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<any>(`${this.url}/ventas`, { params });
  }

  ranking(): Observable<any[]> {
    return this.http.get<any[]>(`${this.url}/ranking`);
  }

  evolucion(dias: number = 7): Observable<any[]> {
    const params = new HttpParams().set('dias', dias.toString());
    return this.http.get<any[]>(`${this.url}/evolucion`, { params });
  }

  demanda(limit: number = 5): Observable<any[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<any[]>(`${this.url}/demanda`, { params });
  }

  mensual(meses: number = 6, sucursalId?: number | null, productoId?: number | null): Observable<any[]> {
    let params = new HttpParams().set('meses', meses.toString());
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId.toString());
    }
    if (productoId) {
      params = params.set('productoId', productoId.toString());
    }
    return this.http.get<any[]>(`${this.url}/mensual`, { params });
  }

  prediccion(mesesHistoricos: number = 6, sucursalId?: number | null, productoId?: number | null): Observable<any[]> {
    let params = new HttpParams().set('mesesHistoricos', mesesHistoricos.toString());
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId.toString());
    }
    if (productoId) {
      params = params.set('productoId', productoId.toString());
    }
    return this.http.get<any[]>(`${this.url}/prediccion`, { params });
  }
}
