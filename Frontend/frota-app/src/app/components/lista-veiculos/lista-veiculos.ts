import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';
import { PagedResponse } from '../../services/paged-response';
import { AuthService } from '../../services/auth.service';

interface Veiculo {
  id?: number;
  placa: string;
  modelo: string;
  marca: string;
  cor?: string;
  frota?: string;
  renavan?: string;
  tipo: string;
}

interface Motorista {
  id?: number;
  nome: string;
  cnh: string;
  categoria?: string;
}

interface VinculoMotorista {
  motoristaId: number;
  motoristaNome: string;
  motoristaCnh: string;
  dataHoraInicio: string;
  dataHoraFim?: string;
  ativo: boolean;
}

@Component({
  imports: [CommonModule, MatGridListModule, RouterLink, MatIconModule, ReactiveFormsModule],
  selector: 'app-lista-veiculos',
  styleUrl: './lista-veiculos.css',
  templateUrl: './lista-veiculos.html',
})
export class ListaVeiculos implements OnInit {
  readonly tipos = ['PASSEIO', 'CARGA', 'UTILITARIO', 'MOTOCICLETA', 'VAN', 'CAVALO', 'CARRETA', 'ONIBUS'];
  readonly veiculoForm;
  veiculos: Veiculo[] = [];
  modalAberto = false;
  carregando = false;
  salvando = false;
  idEmEdicao?: number;
  paginaAtual = 1;
  readonly itensPorPagina = 5;
  totalPaginas = 1;
  totalElementos = 0;
  motoristas: Motorista[] = [];
  motoristasVinculados = new Set<number>();
  veiculoSelecionado?: Veiculo;
  modalMotoristasAberto = false;
  carregandoMotoristas = false;
  alterandoVinculo?: number;
  vinculosPorVeiculo = new Map<number, VinculoMotorista[]>();
  termoBusca = '';
  readonly isAssistente: boolean;
  private buscaAgendada?: ReturnType<typeof setTimeout>;

  get veiculosPaginados(): Veiculo[] {
    return this.veiculos;
  }

  constructor(
    private formBuilder: FormBuilder,
    private http: HttpClient,
    private changeDetector: ChangeDetectorRef,
    authService: AuthService,
    dataRefresh: AppDataRefreshService
  ) {
    this.isAssistente = authService.hasAnyRole('ASSISTENTE_FROTA') && !authService.isAdmin;
    this.veiculoForm = this.formBuilder.group({
      placa: ['', [Validators.required, Validators.minLength(7), Validators.maxLength(8)]],
      modelo: ['', Validators.required],
      marca: ['', Validators.required],
      cor: [''],
      frota: [''],
      renavan: [''],
      tipo: ['', Validators.required]
    });
    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => this.carregarVeiculos());

  }

  ngOnInit(): void {
    this.carregarVeiculos();
  }

  carregarVeiculos(): void {
    this.carregando = true;
    const params: Record<string, string> = {
      page: String(this.paginaAtual - 1),
      size: String(this.itensPorPagina)
    };
    if (this.termoBusca.trim()) params['busca'] = this.termoBusca.trim();
    this.http.get<PagedResponse<Veiculo>>('http://localhost:8080/api/veiculos', { params }).subscribe({
      next: resposta => {
        this.totalPaginas = Math.max(1, resposta.totalPages);
        this.totalElementos = resposta.totalElements;
        if (this.paginaAtual > this.totalPaginas) {
          this.paginaAtual = this.totalPaginas;
          this.carregarVeiculos();
          return;
        }
        this.veiculos = resposta.content;
        this.carregando = false;
        if (!this.isAssistente) this.carregarVinculos(resposta.content);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.changeDetector.markForCheck();
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar',
          text: 'Não foi possível carregar os veículos. Verifique se a API está disponível.'
        });
      }
    });
  }

  buscar(termo: string): void {
    this.termoBusca = termo;
    this.paginaAtual = 1;
    clearTimeout(this.buscaAgendada);
    this.buscaAgendada = setTimeout(() => this.carregarVeiculos(), 300);
  }

  limparBusca(): void {
    if (!this.termoBusca) return;
    this.buscar('');
  }

  carregarVinculos(veiculos: Veiculo[]): void {
    const veiculosComId = veiculos.filter(veiculo => veiculo.id !== undefined);
    if (veiculosComId.length === 0) return;
    forkJoin(veiculosComId.map(veiculo => this.http.get<VinculoMotorista[]>(`http://localhost:8080/api/veiculos/${veiculo.id}/vinculos-motoristas`))).subscribe({
      next: respostas => {
        respostas.forEach((vinculos, indice) => this.vinculosPorVeiculo.set(veiculosComId[indice].id as number, vinculos));
        this.changeDetector.markForCheck();
      },
      error: () => { /* A tabela continua disponível mesmo sem o histórico. */ }
    });
  }

  motoristaAtual(veiculo: Veiculo): VinculoMotorista | undefined {
    return veiculo.id ? this.vinculosPorVeiculo.get(veiculo.id)?.find(vinculo => vinculo.ativo) : undefined;
  }

  abrirModal(): void {
    this.veiculoForm.reset();
    this.idEmEdicao = undefined;
    this.modalAberto = true;
  }

  editarVeiculo(veiculo: Veiculo): void {
    this.idEmEdicao = veiculo.id;
    this.veiculoForm.patchValue(veiculo);
    this.veiculoForm.markAsPristine();
    this.modalAberto = true;
  }

  fecharModal(): void {
    if (this.salvando) {
      return;
    }

    if (!this.veiculoForm.dirty) {
      this.modalAberto = false;
      return;
    }

    Swal.fire({
      icon: 'warning',
      title: 'Descartar cadastro?',
      text: 'Os dados preenchidos serão perdidos.',
      showCancelButton: true,
      confirmButtonText: 'Descartar',
      cancelButtonText: 'Continuar preenchendo',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (result.isConfirmed) {
        this.veiculoForm.reset();
        this.idEmEdicao = undefined;
        this.modalAberto = false;
      }
    });
  }

  salvarVeiculo(): void {
    if (this.veiculoForm.invalid) {
      this.veiculoForm.markAllAsTouched();
      Swal.fire({
        icon: 'warning',
        title: 'Dados incompletos',
        text: 'Preencha placa, modelo, marca e tipo para cadastrar o veículo.'
      });
      return;
    }

    this.salvando = true;
    const dados = this.veiculoForm.getRawValue();
    const requisicao = this.idEmEdicao
      ? this.http.put<Veiculo>(`http://localhost:8080/api/veiculos/${this.idEmEdicao}`, dados)
      : this.http.post<Veiculo>('http://localhost:8080/api/veiculos', dados);

    requisicao.pipe(
      timeout(10000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: () => {
        if (!this.idEmEdicao) {
          this.paginaAtual = Math.max(
            1,
            Math.ceil((this.totalElementos + 1) / this.itensPorPagina)
          );
        }
        this.salvando = false;
        this.veiculoForm.reset();
        this.modalAberto = false;
        const acao = this.idEmEdicao ? 'atualizado' : 'cadastrado';
        this.idEmEdicao = undefined;
        this.carregarVeiculos();
        Swal.fire({
          icon: 'success',
          title: `Veículo ${acao}`,
          text: `O veículo foi ${acao} com sucesso.`,
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: () => {
        Swal.fire({
          icon: 'error',
          title: 'Erro ao cadastrar',
          text: 'Não foi possível cadastrar o veículo. Verifique os dados e a disponibilidade da API.'
        });
      }
    });
  }

  excluirVeiculo(veiculo: Veiculo): void {
    if (!veiculo.id) {
      return;
    }

    Swal.fire({
      icon: 'warning',
      title: 'Excluir veículo?',
      text: `O veículo ${veiculo.placa} será removido da frota.`,
      showCancelButton: true,
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }

      this.http.delete(`http://localhost:8080/api/veiculos/${veiculo.id}`).pipe(timeout(10000)).subscribe({
        next: () => {
          this.carregarVeiculos();
          Swal.fire({
            icon: 'success',
            title: 'Veículo excluído',
            timer: 1800,
            showConfirmButton: false
          });
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Erro ao excluir',
            text: 'Não foi possível excluir o veículo.'
          });
        }
      });
    });
  }

  abrirMotoristas(veiculo: Veiculo): void {
    if (!veiculo.id) return;
    this.veiculoSelecionado = veiculo;
    this.modalMotoristasAberto = true;
    this.carregandoMotoristas = true;
    forkJoin({
      todos: this.http.get<Motorista[]>('http://localhost:8080/api/motoristas'),
      vinculos: this.http.get<VinculoMotorista[]>(`http://localhost:8080/api/veiculos/${veiculo.id}/vinculos-motoristas`)
    }).subscribe({
      next: resposta => {
        this.motoristas = resposta.todos;
        this.vinculosPorVeiculo.set(veiculo.id as number, resposta.vinculos);
        this.motoristasVinculados = new Set(resposta.vinculos.filter(item => item.ativo).map(item => item.motoristaId));
        this.carregandoMotoristas = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregandoMotoristas = false;
        this.changeDetector.markForCheck();
        Swal.fire({ icon: 'error', title: 'Erro ao carregar motoristas', text: 'Não foi possível consultar os vínculos deste veículo.' });
      }
    });
  }

  fecharMotoristas(): void {
    if (!this.alterandoVinculo) this.modalMotoristasAberto = false;
  }

  estaVinculado(motorista: Motorista): boolean {
    return motorista.id !== undefined && this.motoristasVinculados.has(motorista.id);
  }

  alterarVinculo(motorista: Motorista): void {
    if (!this.veiculoSelecionado?.id || !motorista.id) return;
    const vinculado = this.estaVinculado(motorista);
    this.alterandoVinculo = motorista.id;
    const url = `http://localhost:8080/api/veiculos/${this.veiculoSelecionado.id}/motoristas/${motorista.id}`;
    const requisicao = vinculado ? this.http.delete(url) : this.http.put(url, {});
    requisicao.pipe(timeout(10000), finalize(() => {
      this.alterandoVinculo = undefined;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => {
        if (vinculado) this.motoristasVinculados.delete(motorista.id as number);
        else this.motoristasVinculados.add(motorista.id as number);
        this.motoristasVinculados = new Set(this.motoristasVinculados);
        this.carregarVinculos([this.veiculoSelecionado as Veiculo]);
        Swal.fire({ icon: 'success', title: vinculado ? 'Vínculo removido' : 'Motorista vinculado', timer: 1500, showConfirmButton: false });
      },
      error: (erro: { error?: { message?: string } }) => Swal.fire({ icon: 'error', title: 'Erro no vínculo', text: erro.error?.message || 'Não foi possível alterar o motorista deste veículo.' })
    });
  }

  mudarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaAtual = pagina;
      this.carregarVeiculos();
    }
  }
}
