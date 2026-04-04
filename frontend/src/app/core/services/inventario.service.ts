import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, interval, merge, tap, switchMap, catchError, of, shareReplay } from 'rxjs';
import { AjusteInventarioRequest, InventarioItem } from '../models/inventario.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private url = `${environment.apiUrl}/inventario`;
  private refreshSignal$ = new BehaviorSubject<void>(undefined);

  constructor(private http: HttpClient) {}

  /**
   * Obtiene un flujo reactivo del inventario que se actualiza automáticamente 
   * ante cambios locales (CRUD) o cada 10 segundos (polling).
   */
  getInventario(sucursalId: number | null): Observable<InventarioItem[]> {
    return merge(
      this.refreshSignal$,
      interval(10000)
    ).pipe(
      switchMap(() => {
        if (sucursalId === 0 || sucursalId === null) {
          return this.listarTodo();
        }
        return this.listarPorSucursal(sucursalId);
      }),
      shareReplay(1),
      catchError(err => {
        console.error('Error en sync de inventario:', err);
        return of([]);
      })
    );
  }

  /**
   * Dispara una actualización inmediata del flujo de datos
   */
  notifyChange() {
    this.refreshSignal$.next();
  }

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
    return this.http.post<InventarioItem>(`${this.url}/ajustar`, req).pipe(
      tap(() => this.notifyChange())
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`).pipe(
      tap(() => this.notifyChange())
    );
  }
}
