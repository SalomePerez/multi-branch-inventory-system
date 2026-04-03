import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DescuentoDto {
  id: number;
  nombre: string;
  descripcion: string;
  porcentaje: number;
  condicionCantidadMinima: number;
  activa: boolean;
  categoriaIds: number[];
}

export interface DescuentoRequest {
  nombre: string;
  descripcion: string;
  porcentaje: number;
  condicionCantidadMinima: number;
  activa: boolean;
  categoriaIds: number[];
}

@Injectable({ providedIn: 'root' })
export class ListaPrecioService {
  private url = `${environment.apiUrl}/listas-precio`;

  constructor(private http: HttpClient) {}

  todas(): Observable<DescuentoDto[]> {
    return this.http.get<DescuentoDto[]>(`${this.url}`);
  }

  activas(): Observable<DescuentoDto[]> {
    return this.http.get<DescuentoDto[]>(`${this.url}/activas`);
  }

  crear(req: DescuentoRequest): Observable<DescuentoDto> {
    return this.http.post<DescuentoDto>(`${this.url}`, req);
  }

  actualizar(id: number, req: DescuentoRequest): Observable<DescuentoDto> {
    return this.http.put<DescuentoDto>(`${this.url}/${id}`, req);
  }

  alternarEstado(id: number): Observable<void> {
    return this.http.put<void>(`${this.url}/${id}/toggle`, {});
  }
}
