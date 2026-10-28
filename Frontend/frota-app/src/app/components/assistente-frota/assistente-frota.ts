import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { finalize, forkJoin, timeout } from 'rxjs';
import Swal from 'sweetalert2';
import { AuthService } from '../../services/auth.service';

interface Veiculo {
  id: number;
  placa: string;
  marca: string;
  modelo: string;
  frota?: string;
}

interface Oficina {
  id: number;
  nomeOficina: string;
  telefone: string;
  especialidade: string;
  endereco?: {
    logradouro: string;
    numero: string;
    bairro: string;
    cidade: string;
    estado: string;
    cep: string;
  };
}

interface Encaminhamento {
  id: number;
  veiculo: Veiculo;
  oficina: Oficina;
  orientacao?: string;
  enviadoEm: string;
}

@Component({
  selector: 'app-assistente-frota',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './assistente-frota.html',
  styleUrl: './assistente-frota.css'
})
export class AssistenteFrota implements OnInit {
  readonly contaForm;
  readonly encaminhamentoForm;
  readonly isAdmin: boolean;
  veiculos: Veiculo[] = [];
  oficinas: Oficina[] = [];
  encaminhamentos: Encaminhamento[] = [];
  carregando = true;
  erroCarregamento = false;
  criandoConta = false;
  enviandoAlerta = false;

  constructor(
    formBuilder: FormBuilder,
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef,
    authService: AuthService
  ) {
    this.isAdmin = authService.isAdmin;
    this.contaForm = formBuilder.nonNullable.group({
      username: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]]
    });
    this.encaminhamentoForm = formBuilder.nonNullable.group({
      veiculoId: ['', Validators.required],
      oficinaId: ['', Validators.required],
      orientacao: ['', Validators.maxLength(1000)]
    });
  }

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    this.erroCarregamento = false;
    forkJoin({
      veiculos: this.http.get<Veiculo[]>('http://localhost:8080/api/veiculos'),
      oficinas: this.http.get<Oficina[]>('http://localhost:8080/api/oficinas'),
      encaminhamentos: this.http.get<Encaminhamento[]>('http://localhost:8080/api/assistente/encaminhamentos')
    }).subscribe({
      next: dados => {
        this.veiculos = dados.veiculos;
        this.oficinas = dados.oficinas;
        const encaminhamentos = dados.encaminhamentos;
        this.encaminhamentos = encaminhamentos;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => this.finalizarErroCarregamento('Não foi possível carregar os veículos, oficinas ou alertas de manutenção.')
    });
  }

  private finalizarErroCarregamento(mensagem: string): void {
    this.carregando = false;
    this.erroCarregamento = true;
    this.changeDetector.markForCheck();
    void Swal.fire({ icon: 'error', title: 'Erro ao carregar', text: mensagem });
  }

  criarConta(): void {
    if (this.contaForm.invalid) {
      this.contaForm.markAllAsTouched();
      return;
    }

    this.criandoConta = true;
    this.http.post('http://localhost:8080/api/admin/assistentes', this.contaForm.getRawValue()).pipe(
      timeout(10000),
      finalize(() => {
        this.criandoConta = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.contaForm.reset();
        void Swal.fire({
          icon: 'success',
          title: 'Conta criada',
          text: 'O novo usuário já pode acessar como assistente de frota.',
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: (erro: { error?: { message?: string } }) => void Swal.fire({
        icon: 'error',
        title: 'Não foi possível criar a conta',
        text: erro.error?.message || 'Confira os dados e tente novamente.'
      })
    });
  }

  enviarAlerta(): void {
    if (this.encaminhamentoForm.invalid) {
      this.encaminhamentoForm.markAllAsTouched();
      return;
    }
    if (this.encaminhamentos.some(item => String(item.veiculo.id) === this.encaminhamentoForm.controls.veiculoId.value)) {
      void Swal.fire({
        icon: 'warning',
        title: 'Alerta já enviado',
        text: 'Conclua o encaminhamento atual antes de enviar outro para este veículo.'
      });
      return;
    }

    this.enviandoAlerta = true;
    this.http.post<Encaminhamento>(
      'http://localhost:8080/api/assistente/encaminhamentos',
      this.encaminhamentoForm.getRawValue()
    ).pipe(
      timeout(10000),
      finalize(() => {
        this.enviandoAlerta = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: encaminhamento => {
        this.encaminhamentos = [encaminhamento, ...this.encaminhamentos];
        this.encaminhamentoForm.reset({ veiculoId: '', oficinaId: '', orientacao: '' });
        void Swal.fire({
          icon: 'success',
          title: 'Motorista avisado',
          text: 'O destino da manutenção já está disponível em Minhas rotas.',
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: (erro: { error?: { message?: string } }) => void Swal.fire({
        icon: 'error',
        title: 'Não foi possível enviar o alerta',
        text: erro.error?.message || 'Confira se o veículo está atribuído a um motorista.'
      })
    });
  }

  concluir(encaminhamento: Encaminhamento): void {
    void Swal.fire({
      icon: 'question',
      title: 'Encerrar encaminhamento?',
      text: `O alerta para ${encaminhamento.veiculo.placa} será removido da área do motorista.`,
      showCancelButton: true,
      confirmButtonText: 'Encerrar',
      cancelButtonText: 'Manter alerta'
    }).then(resultado => {
      if (!resultado.isConfirmed) return;
      this.http.delete(`http://localhost:8080/api/assistente/encaminhamentos/${encaminhamento.id}`).pipe(
        timeout(10000)
      ).subscribe({
        next: () => this.encaminhamentos = this.encaminhamentos.filter(item => item.id !== encaminhamento.id),
        error: () => void Swal.fire({
          icon: 'error',
          title: 'Não foi possível encerrar',
          text: 'Tente novamente.'
        })
      });
    });
  }
}
