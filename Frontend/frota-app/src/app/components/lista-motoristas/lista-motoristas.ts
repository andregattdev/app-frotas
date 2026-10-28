import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';
import { PagedResponse } from '../../services/paged-response';

interface Motorista {
  id?: number;
  nome: string;
  cnh: string;
  categoria: string;
  validadeCnh: string;
  telefone?: string;
  endereco?: Endereco;
}

interface Endereco {
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
}

interface ViaCepResposta { cep?: string; logradouro?: string; bairro?: string; localidade?: string; uf?: string; erro?: boolean; }

@Component({
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, RouterLink],
  selector: 'app-lista-motoristas',
  styleUrl: './lista-motoristas.css',
  templateUrl: './lista-motoristas.html',
})
export class ListaMotoristas implements OnInit {
  readonly motoristaForm;
  readonly contaForm;
  motoristas: Motorista[] = [];
  modalAberto = false;
  carregando = false;
  salvando = false;
  idEmEdicao?: number;
  paginaAtual = 1;
  readonly itensPorPagina = 5;
  totalPaginas = 1;
  totalElementos = 0;
  buscandoCep = false;
  motoristaParaConta?: Motorista;
  criandoConta = false;
  mostrarSenhaInicial = false;
  termoBusca = '';
  private buscaAgendada?: ReturnType<typeof setTimeout>;

  get motoristasPaginados(): Motorista[] {
    return this.motoristas;
  }

  constructor(
    private formBuilder: FormBuilder,
    private http: HttpClient,
    private changeDetector: ChangeDetectorRef,
    dataRefresh: AppDataRefreshService
  ) {
    this.motoristaForm = this.formBuilder.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      cnh: ['', [Validators.required, Validators.minLength(11), Validators.maxLength(11)]],
      categoria: ['', Validators.required],
      validadeCnh: ['', Validators.required],
      telefone: [''],
      logradouro: [''], numero: [''], complemento: [''], bairro: [''], cidade: [''], estado: ['', Validators.maxLength(2)], cep: ['']
    });
    this.contaForm = this.formBuilder.nonNullable.group({
      username: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]]
    });
    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => this.carregarMotoristas());
  }

  ngOnInit(): void {
    this.carregarMotoristas();
  }

  carregarMotoristas(): void {
    this.carregando = true;
    const params: Record<string, string> = {
      page: String(this.paginaAtual - 1),
      size: String(this.itensPorPagina)
    };
    if (this.termoBusca.trim()) params['busca'] = this.termoBusca.trim();
    this.http.get<PagedResponse<Motorista>>('http://localhost:8080/api/motoristas', { params }).subscribe({
      next: resposta => {
        this.totalPaginas = Math.max(1, resposta.totalPages);
        this.totalElementos = resposta.totalElements;
        if (this.paginaAtual > this.totalPaginas) {
          this.paginaAtual = this.totalPaginas;
          this.carregarMotoristas();
          return;
        }
        this.motoristas = resposta.content;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.changeDetector.markForCheck();
        Swal.fire({ icon: 'error', title: 'Erro ao carregar', text: 'Não foi possível carregar os motoristas.' });
      }
    });
  }

  buscar(termo: string): void { this.termoBusca = termo; this.paginaAtual = 1; clearTimeout(this.buscaAgendada); this.buscaAgendada = setTimeout(() => this.carregarMotoristas(), 300); }
  limparBusca(): void { if (this.termoBusca) this.buscar(''); }

  abrirModal(): void { this.motoristaForm.reset(); this.idEmEdicao = undefined; this.modalAberto = true; }

  abrirModalConta(motorista: Motorista): void {
    this.motoristaParaConta = motorista;
    this.mostrarSenhaInicial = false;
    this.contaForm.reset();
  }

  fecharModalConta(): void {
    if (this.criandoConta) return;
    this.motoristaParaConta = undefined;
    this.mostrarSenhaInicial = false;
    this.contaForm.reset();
  }

  criarContaMotorista(): void {
    if (!this.motoristaParaConta?.id || this.criandoConta) {
      return;
    }
    if (this.contaForm.invalid) {
      this.contaForm.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Dados incompletos', text: 'Informe usuário, e-mail válido e senha de 12 a 72 caracteres.' });
      return;
    }
    this.criandoConta = true;
    this.http.post('http://localhost:8080/api/admin/motoristas/' + this.motoristaParaConta.id + '/conta', this.contaForm.getRawValue())
      .pipe(timeout(10000), finalize(() => {
        this.criandoConta = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: () => {
          this.motoristaParaConta = undefined;
          this.mostrarSenhaInicial = false;
          this.contaForm.reset();
          Swal.fire({ icon: 'success', title: 'Acesso criado', text: 'A conta foi vinculada ao motorista. Compartilhe a senha com segurança.', timer: 2400 });
        },
        error: () => Swal.fire({ icon: 'error', title: 'Não foi possível criar o acesso', text: 'Verifique se o motorista já possui uma conta, se os dados estão válidos e se seu perfil é de administrador.' })
      });
  }

  editarMotorista(motorista: Motorista): void {
    this.idEmEdicao = motorista.id;
    this.motoristaForm.patchValue({ ...motorista, ...motorista.endereco });
    this.motoristaForm.markAsPristine();
    this.modalAberto = true;
  }

  buscarCep(): void {
    const cep = String(this.motoristaForm.controls.cep.value || '').replace(/\D/g, '');
    if (cep.length !== 8) return;
    this.buscandoCep = true;
    this.http.get<ViaCepResposta>(`http://localhost:8080/api/enderecos/consulta-cep/${cep}`).pipe(timeout(8000), finalize(() => {
      this.buscandoCep = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: resposta => {
        if (resposta.erro) { Swal.fire({ icon: 'warning', title: 'CEP não encontrado', text: 'Confira o CEP informado.' }); return; }
        this.motoristaForm.patchValue({ cep: resposta.cep, logradouro: resposta.logradouro, bairro: resposta.bairro, cidade: resposta.localidade, estado: resposta.uf });
      },
      error: () => Swal.fire({ icon: 'error', title: 'Erro ao consultar CEP', text: 'Não foi possível consultar o ViaCEP.' })
    });
  }

  fecharModal(): void {
    if (this.salvando) return;
    if (!this.motoristaForm.dirty) { this.modalAberto = false; return; }
    Swal.fire({ icon: 'warning', title: 'Descartar cadastro?', text: 'Os dados preenchidos serão perdidos.', showCancelButton: true, confirmButtonText: 'Descartar', cancelButtonText: 'Continuar', confirmButtonColor: '#b91c1c' }).then(result => {
      if (result.isConfirmed) { this.motoristaForm.reset(); this.modalAberto = false; }
    });
  }

  salvarMotorista(): void {
    if (this.motoristaForm.invalid) { this.motoristaForm.markAllAsTouched(); Swal.fire({ icon: 'warning', title: 'Dados incompletos', text: 'Preencha nome, CNH, categoria e validade.' }); return; }
    const dados = this.motoristaForm.getRawValue();
    const payload = { nome: dados.nome, cnh: dados.cnh, categoria: dados.categoria, validadeCnh: dados.validadeCnh, telefone: dados.telefone, endereco: dados.logradouro ? { logradouro: dados.logradouro, numero: dados.numero, complemento: dados.complemento, bairro: dados.bairro, cidade: dados.cidade, estado: dados.estado, cep: dados.cep } : null };
    this.salvando = true;
    const requisicao = this.idEmEdicao ? this.http.put<Motorista>(`http://localhost:8080/api/motoristas/${this.idEmEdicao}`, payload) : this.http.post<Motorista>('http://localhost:8080/api/motoristas', payload);
    requisicao.pipe(timeout(10000), finalize(() => {
      this.salvando = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => {
        if (!this.idEmEdicao) {
          this.paginaAtual = Math.max(1, Math.ceil((this.totalElementos + 1) / this.itensPorPagina));
        }
        this.modalAberto = false;
        this.motoristaForm.reset();
        this.idEmEdicao = undefined;
        this.carregarMotoristas();
        Swal.fire({ icon: 'success', title: 'Motorista salvo', timer: 1800, showConfirmButton: false });
      },
      error: () => Swal.fire({ icon: 'error', title: 'Erro ao salvar', text: 'Não foi possível salvar o motorista.' })
    });
  }

  excluirMotorista(motorista: Motorista): void {
    Swal.fire({ icon: 'warning', title: 'Excluir motorista?', text: `${motorista.nome} será removido.`, showCancelButton: true, confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar', confirmButtonColor: '#b91c1c' }).then(result => {
      if (!result.isConfirmed || !motorista.id) return;
      this.http.delete(`http://localhost:8080/api/motoristas/${motorista.id}`).pipe(timeout(10000)).subscribe({
        next: () => {
          this.carregarMotoristas();
          Swal.fire({ icon: 'success', title: 'Motorista excluído', timer: 1600, showConfirmButton: false });
        },
        error: () => Swal.fire({ icon: 'error', title: 'Erro ao excluir', text: 'Não foi possível excluir o motorista.' })
      });
    });
  }

  mudarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaAtual = pagina;
      this.carregarMotoristas();
    }
  }
}
