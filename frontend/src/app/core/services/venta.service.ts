import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Venta, VentaRequest } from '../models/venta.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private url = `${environment.apiUrl}/ventas`;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<Venta[]> {
    return this.http.get<Venta[]>(this.url);
  }

  listarPorSucursal(sucursalId: number): Observable<Venta[]> {
    return this.http.get<Venta[]>(`${this.url}/sucursal/${sucursalId}`);
  }

  obtener(id: number): Observable<Venta> {
    return this.http.get<Venta>(`${this.url}/${id}`);
  }

  registrar(req: VentaRequest): Observable<Venta> {
    return this.http.post<Venta>(this.url, req);
  }
}
