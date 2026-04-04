import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria, Producto, ProductoRequest, UnidadMedida } from '../models/producto.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductoService {
  private url = `${environment.apiUrl}/productos`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.url);
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.url}/${id}`);
  }

  crear(req: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.url, req);
  }

  actualizar(id: number, req: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.url}/${id}`, req);
  }

  desactivar(id: number, motivo: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}?motivo=${encodeURIComponent(motivo)}`);
  }

  listarUnidades(): Observable<UnidadMedida[]> {
    return this.http.get<UnidadMedida[]>(`${this.url}/unidades`);
  }

  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.url}/categorias`);
  }

  crearCategoria(cat: Partial<Categoria>): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.url}/categorias`, cat);
  }
}
