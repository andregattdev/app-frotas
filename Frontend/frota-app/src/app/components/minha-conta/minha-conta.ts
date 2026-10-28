import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-minha-conta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './minha-conta.html',
  styleUrl: './minha-conta.css'
})
export class MinhaConta {
  readonly form;
  mostrarSenhaAtual = false;
  mostrarNovaSenha = false;
  mostrarConfirmacao = false;
  salvando = false;
  mensagemErro = '';
  mensagemSucesso = '';

  constructor(
    formBuilder: FormBuilder,
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef
  ) {
    this.form = formBuilder.nonNullable.group({
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
      confirmarSenha: ['', Validators.required]
    });
  }

  alterarSenha(): void {
    this.mensagemErro = '';
    this.mensagemSucesso = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { senhaAtual, novaSenha, confirmarSenha } = this.form.getRawValue();
    if (novaSenha !== confirmarSenha) {
      this.mensagemErro = 'A nova senha e a confirmação não correspondem.';
      return;
    }

    this.salvando = true;
    this.http.post<void>('http://localhost:8080/api/auth/alterar-senha', { senhaAtual, novaSenha }).pipe(
      timeout(10000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.form.reset();
        this.mostrarSenhaAtual = false;
        this.mostrarNovaSenha = false;
        this.mostrarConfirmacao = false;
        this.mensagemSucesso = 'Senha alterada com sucesso.';
      },
      error: (error: unknown) => {
        this.mensagemErro = this.mensagemDaApi(error);
      }
    });
  }

  private mensagemDaApi(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'Não foi possível alterar a senha. Verifique sua conexão e tente novamente.';
  }
}
