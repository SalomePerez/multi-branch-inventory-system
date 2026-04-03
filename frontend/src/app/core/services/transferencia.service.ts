import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transferencia, TransferenciaRequest } from '../models/transferencia.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TransferenciaService {
  private url = `${environment.apiUrl}/transferencias`;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<Transferencia[]> {
    return this.http.get<Transferencia[]>(this.url);
  }

  listarPorSucursal(sucursalId: number): Observable<Transferencia[]> {
    return this.http.get<Transferencia[]>(`${this.url}/sucursal/${sucursalId}`);
  }

  obtener(id: number): Observable<Transferencia> {
    return this.http.get<Transferencia>(`${this.url}/${id}`);
  }

  solicitar(req: TransferenciaRequest): Observable<Transferencia> {
    return this.http.post<Transferencia>(this.url, req);
  }

  aprobar(id: number): Observable<Transferencia> {
    return this.http.patch<Transferencia>(`${this.url}/${id}/aprobar`, {});
  }

  rechazar(id: number, motivo: string): Observable<Transferencia> {
    return this.http.patch<Transferencia>(`${this.url}/${id}/rechazar`, { motivo });
  }

  enviar(id: number, cantidadesEnviadas: number[], transportista: string): Observable<Transferencia> {
    return this.http.patch<Transferencia>(`${this.url}/${id}/enviar`, { cantidadesEnviadas, transportista });
  }

  recibir(id: number, cantidades: number[]): Observable<Transferencia> {
    return this.http.patch<Transferencia>(`${this.url}/${id}/recibir`, cantidades);
  }
}
