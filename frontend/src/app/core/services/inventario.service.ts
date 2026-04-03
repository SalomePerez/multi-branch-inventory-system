import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AjusteInventarioRequest, InventarioItem } from '../models/inventario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private url = `${environment.apiUrl}/inventario`;

  constructor(private http: HttpClient) {}

  listarTodo(): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(this.url);
  }

  listarPorSucursal(sucursalId: number): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(`${this.url}/sucursal/${sucursalId}`);
  }

  stockBajo(): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(`${this.url}/stock-bajo`);
  }

  ajustar(req: AjusteInventarioRequest): Observable<InventarioItem> {
    return this.http.post<InventarioItem>(`${this.url}/ajustar`, req);
  }
}
