import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { importProvidersFrom } from '@angular/core';
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

export const appConfig: ApplicationConfig = {
  providers: [
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
