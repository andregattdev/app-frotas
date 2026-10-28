import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PwaInstallService } from '../../services/pwa-install.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-menu-lateral',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  templateUrl: './menu-lateral.html',
  styleUrl: './menu-lateral.css'
})
export class MenuLateral {
  [x: string]: any;
  menuAberto = false;

  constructor(
    public readonly authService: AuthService,
    public readonly pwaService: PwaInstallService,
    private readonly router: Router
  ) { }

  get isPerfilOperacional(): boolean {
    return !this.authService.isAdmin && this.authService.hasAnyRole('MOTORISTA', 'ASSISTENTE_FROTA');
  }

  get isMotorista(): boolean {
    return !this.authService.isAdmin && this.authService.hasAnyRole('MOTORISTA');
  }

  get isAssistenteFrota(): boolean {
    return !this.authService.isAdmin && this.authService.hasAnyRole('ASSISTENTE_FROTA');
  }



  get usuarioLogado(): string {
    return this.authService.username ?? '';
  }

  get inicialUsuario(): string {
    return this.usuarioLogado.charAt(0).toUpperCase();
  }


  get cargoUsuario(): string {
    const role = this.authService.roles[0];

    const cargos: Record<string, string> = {
      SUPER_ADMIN: 'Super Administrador',
      ADMIN: 'Administrador',
      ASSISTENTE_FROTA: 'Assistente de Frota',
      GESTOR_FROTA: 'Gestor de Frota',
      OPERADOR: 'Operador',
      MOTORISTA: 'Motorista',
      MECANICO: 'Mecânico',
      FINANCEIRO: 'Financeiro',
      USER: 'Usuário'
    };

    return cargos[role] || 'Usuário';
  }



  fecharMenu(): void {
    this.menuAberto = false;
  }

  sair(): void {
    this.authService.logout();
    this.fecharMenu();
    void this.router.navigate(['/login']);
  }
}
