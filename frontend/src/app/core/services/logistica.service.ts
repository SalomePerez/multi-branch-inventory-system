import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, interval, merge, tap, switchMap, shareReplay, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LogisticaService {
  private url = `${environment.apiUrl}/logistica`;
  private refreshSignal$ = new BehaviorSubject<void>(undefined);

  constructor(private http: HttpClient) {}

  /**
   * Obtiene un flujo reactivo de envíos que se actualiza automáticamente 
   * ante acciones locales o cada 10 segundos.
   */
  getEnviosStream(estado?: string): Observable<any[]> {
    return merge(
      this.refreshSignal$,
      interval(10000)
    ).pipe(
      switchMap(() => this.enviosActivos(estado)),
      shareReplay(1),
      catchError(err => {
        console.error('Error en sync de logística:', err);
        return of([]);
      })
    );
  }

  /**
   * Obtiene un flujo reactivo de estadísticas logísticas.
   */
  getStatsStream(): Observable<any> {
    return merge(
      this.refreshSignal$,
      interval(10000)
    ).pipe(
      switchMap(() => this.getStats()),
      shareReplay(1)
    );
  }

  notifyChange() {
    this.refreshSignal$.next();
  }

  enviosActivos(estado?: string): Observable<any[]> {
    let params: { [param: string]: string } = {};
    if (estado) {
      params['estado'] = estado;
    }
    return this.http.get<any[]>(`${this.url}/envios-activos`, { params });
  }

  despachar(id: number, req: any): Observable<void> {
    return this.http.post<void>(`${this.url}/despachar/${id}`, req).pipe(
      tap(() => this.notifyChange())
    );
  }

  rechazar(id: number, motivo: string): Observable<void> {
    return this.http.post<void>(`${this.url}/rechazar/${id}`, { motivo }).pipe(
      tap(() => this.notifyChange())
    );
  }

  entregar(id: number, body: any): Observable<void> {
    return this.http.post<void>(`${this.url}/entregar/${id}`, body).pipe(
      tap(() => this.notifyChange())
    );
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
