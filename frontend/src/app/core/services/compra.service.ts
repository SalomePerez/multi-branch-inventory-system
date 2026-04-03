import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrdenCompra, OrdenCompraRequest, CppResponse } from '../models/compra.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private url = `${environment.apiUrl}/compras`;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<OrdenCompra[]> {
    return this.http.get<OrdenCompra[]>(this.url);
  }

  listarPorSucursal(sucursalId: number): Observable<OrdenCompra[]> {
    return this.http.get<OrdenCompra[]>(`${this.url}/sucursal/${sucursalId}`);
  }

  obtener(id: number): Observable<OrdenCompra> {
    return this.http.get<OrdenCompra>(`${this.url}/${id}`);
  }

  crear(req: OrdenCompraRequest): Observable<OrdenCompra> {
    return this.http.post<OrdenCompra>(this.url, req);
  }

  aprobar(id: number): Observable<OrdenCompra> {
    return this.http.patch<OrdenCompra>(`${this.url}/${id}/aprobar`, {});
  }

  recibir(id: number): Observable<OrdenCompra> {
    return this.http.patch<OrdenCompra>(`${this.url}/${id}/recibir`, {});
  }

  cancelar(id: number): Observable<OrdenCompra> {
    return this.http.patch<OrdenCompra>(`${this.url}/${id}/cancelar`, {});
  }

  historicoPorProveedor(nombre: string): Observable<OrdenCompra[]> {
    return this.http.get<OrdenCompra[]>(`${this.url}/historico/proveedor`, { params: { nombre } });
  }

  historicoPorProducto(productoId: number): Observable<OrdenCompra[]> {
    return this.http.get<OrdenCompra[]>(`${this.url}/historico/producto/${productoId}`);
  }

  getCpp(productoId: number): Observable<CppResponse> {
    return this.http.get<CppResponse>(`${this.url}/cpp/${productoId}`);
  }
}
