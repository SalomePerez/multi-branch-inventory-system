import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'inventario',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/inventario/inventario.component').then(m => m.InventarioComponent)
          },
          {
            path: 'movimientos',
            loadComponent: () =>
              import('./features/inventario/movimientos/movimientos.component').then(m => m.MovimientosComponent)
          }
        ]
      },
      {
        path: 'productos',
        loadComponent: () =>
          import('./features/productos/productos.component').then(m => m.ProductosComponent)
      },
      {
        path: 'ventas',
        loadComponent: () =>
          import('./features/ventas/ventas.component').then(m => m.VentasComponent)
      },
      {
        path: 'compras',
        loadComponent: () =>
          import('./features/compras/compras.component').then(m => m.ComprasComponent)
      },
      {
        path: 'transferencias',
        loadComponent: () =>
          import('./features/transferencias/transferencias.component').then(m => m.TransferenciasComponent)
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/reportes/reportes.component').then(m => m.ReportesComponent)
      },
      {
        path: 'logistica',
        loadComponent: () =>
          import('./features/logistica/logistica.component').then(m => m.LogisticaComponent)
      },
      {
        path: 'admin',
        loadComponent: () =>
          import('./features/admin/admin.component').then(m => m.AdminComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
