import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { importProvidersFrom } from '@angular/core';
import { AuthService } from './core/services/auth.service';
import { 
  LucideAngularModule, 
  LayoutDashboard, 
  Package, 
  ShoppingCart, 
  ShoppingBag, 
  ArrowLeftRight, 
  BarChart3, 
  Truck, 
  History, 
  Settings, 
  LogOut, 
  User,
  AlertTriangle,
  Bell,
  Building2,
  Search,
  MapPin,
  Calendar,
  CheckCircle2,
  MoreVertical,
  Plus,
  RefreshCw,
  FileText,
  Boxes,
  TrendingUp,
  TrendingDown,
  Info,
  Shield,
  Printer,
  Phone,
  Mail
} from 'lucide-angular';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

/**
 * Inicializa el servicio de autenticación antes de que el router se active.
 * Esto asegura que la identidad del usuario esté disponible durante el bootstrap de rutas.
 */
export function initializeAuth(authService: AuthService) {
  return () => {
    // AuthService ya carga el usuario desde localStorage en su construcción
    // Esta función es un placeholder que asegura que AuthService esté inyectado durante bootstrap
    return Promise.resolve();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    },
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    provideCharts(withDefaultRegisterables()),
    importProvidersFrom(LucideAngularModule.pick({
      LayoutDashboard,
      Package,
      ShoppingCart,
      ShoppingBag,
      ArrowLeftRight,
      BarChart3,
      Truck,
      History,
      Settings,
      LogOut,
      User,
      AlertTriangle,
      Bell,
      Building2,
      Search,
      MapPin,
      Calendar,
      CheckCircle2,
      MoreVertical,
      Plus,
      RefreshCw,
      FileText,
      Boxes,
      TrendingUp,
      TrendingDown,
      Info,
      Shield,
      Printer,
      Phone,
      Mail
    }))
  ]
};
