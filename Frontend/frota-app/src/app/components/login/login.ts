import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { PwaInstallService } from '../../services/pwa-install.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  readonly form;
  enviando = false;
  mostrarSenha = false;
  erro = '';
  readonly acessoNegado: boolean;

  constructor(
    formBuilder: FormBuilder,
    private readonly auth: AuthService,
    public readonly pwaService: PwaInstallService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly changeDetector: ChangeDetectorRef
  ) {
    this.form = formBuilder.nonNullable.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
    this.acessoNegado = this.route.snapshot.queryParamMap.has('acesso');
  }

  entrar(): void {
    if (this.form.invalid || this.enviando) {
      this.form.markAllAsTouched();
      return;
    }

    this.erro = '';
    this.enviando = true;
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).pipe(
      timeout(10000),
      finalize(() => {
        this.enviando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: response => {
        if (!response.roles.includes('ADMIN')
          && !response.roles.includes('SUPER_ADMIN')
          && !response.roles.includes('MOTORISTA')
          && !response.roles.includes('ASSISTENTE_FROTA')) {
          this.auth.logout();
          this.erro = 'Esta conta não possui um perfil habilitado para acessar o aplicativo.';
          return;
        }
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const destination = returnUrl?.startsWith('/') && !returnUrl.startsWith('//')
          ? returnUrl
          : response.roles.includes('ADMIN') || response.roles.includes('SUPER_ADMIN')
            ? '/dashboard'
            : response.roles.includes('MOTORISTA')
            ? '/minhas-rotas'
            : response.roles.includes('ASSISTENTE_FROTA')
              ? '/assistente-frota'
            : '/login';
        void this.router.navigateByUrl(destination);
      },
      error: () => {
        this.erro = 'Usuário ou senha inválidos, ou não foi possível conectar ao servidor.';
      }
    });
  }
}
