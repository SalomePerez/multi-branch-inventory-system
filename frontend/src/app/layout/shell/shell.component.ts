import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AlertaService } from '../../core/services/alerta.service';
import { Alerta } from '../../core/models/alerta.model';
import { LucideAngularModule } from 'lucide-angular';
import { interval, Subscription, startWith, switchMap } from 'rxjs';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  alertas: Alerta[] = [];
  unreadCount = 0;
  mostrarNotificaciones = false;
  filtroGlobal = true;
  private pollingSub?: Subscription;

  constructor(
    public authService: AuthService,
    private alertaService: AlertaService
  ) {}

  ngOnInit() {
    this.iniciarSondeo();
  }

  ngOnDestroy() {
    this.pollingSub?.unsubscribe();
  }

  iniciarSondeo() {
    this.pollingSub = interval(30000) // 30 segundos
      .pipe(
        startWith(0),
        switchMap(() => this.filtroGlobal 
          ? this.alertaService.listarNoLeidas() 
          : this.alertaService.listarPorSucursal(this.authService.user()?.sucursalId!))
      )
      .subscribe(data => {
        if (data.length > this.unreadCount) {
          this.reproducirSonido();
        }
        this.alertas = data;
        this.unreadCount = data.length;
      });
  }

  toggleFiltro() {
    this.filtroGlobal = !this.filtroGlobal;
    this.consultarAhora();
  }

  consultarAhora() {
    const obs = this.filtroGlobal 
      ? this.alertaService.listarNoLeidas() 
      : this.alertaService.listarPorSucursal(this.authService.user()?.sucursalId!);
    
    obs.subscribe(data => {
      this.alertas = data;
      this.unreadCount = data.length;
    });
  }

  leerAlerta(id: number) {
    this.alertaService.marcarLeida(id).subscribe(() => {
      this.consultarAhora();
    });
  }

  leerTodas() {
    this.alertaService.marcarTodasLeidas().subscribe(() => {
      this.consultarAhora();
    });
  }

  reproducirSonido() {
    const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
    audio.play().catch(() => console.log('El navegador bloqueó el sonido automático.'));
  }

  logout() {
    this.authService.logout();
  }
}
