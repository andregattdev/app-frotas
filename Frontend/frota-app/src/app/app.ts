import { Component, signal, WritableSignal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MenuLateral } from './components/menu-lateral/menu-lateral';
import { AppDataRefreshService } from './services/app-data-refresh.service';
import { AuthService } from './services/auth.service';
import { PwaInstallService } from './services/pwa-install.service';

@Component({
  selector: 'app-root',
  imports: [MenuLateral, RouterOutlet, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly isLoginPage: WritableSignal<boolean>;

  constructor(
    private readonly dataRefresh: AppDataRefreshService,
    public readonly authService: AuthService,
    public readonly pwaService: PwaInstallService,
    router: Router
  ) {
    this.isLoginPage = signal(router.url.startsWith('/login'));
    router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(event => this.isLoginPage.set(event.urlAfterRedirects.startsWith('/login')));
  }

  get isPerfilOperacional(): boolean {
    return !this.isLoginPage() && !this.authService.isAdmin && this.authService.hasAnyRole('MOTORISTA', 'ASSISTENTE_FROTA');
  }

  atualizarDadosAoClicar(event: MouseEvent): void {
    const target = event.target;
    if (target instanceof Element && target.closest('button, a[href]')) {
      this.dataRefresh.requestRefresh();
    }
  }
}
