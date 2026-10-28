import { ChangeDetectorRef, Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';
import { AuthService } from '../../services/auth.service';

interface Checklist {
  id?: number;
  dataHora?: string;
  quilometragem?: number;
  pneus?: boolean;
  farois?: boolean;
  lanternas?: boolean;
  freios?: boolean;
  nivelOleo?: boolean;
  nivelAgua?: boolean;
  documentos?: boolean;
  extintor?: boolean;
  triangulo?: boolean;
  macaco?: boolean;
  estepe?: boolean;
  limpeza?: boolean;
  observacoes?: string;
  motorista?: { nome?: string } | null;
}

interface Veiculo {
  id?: number;
  placa?: string;
  modelo?: string;
  frota?: string;
}

interface RotaMotorista {
  veiculo: Veiculo;
}

@Component({
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, RouterLink],
  selector: 'app-lista-checklist',
  styleUrl: './lista-checklist.css',
  templateUrl: './lista-checklist.html',
})
export class ListaChecklist {
  readonly itens = [
    ['pneus', 'Pneus'],
    ['farois', 'Faróis'],
    ['lanternas', 'Lanternas'],
    ['freios', 'Freios'],
    ['nivelOleo', 'Nível do óleo'],
    ['nivelAgua', 'Nível da água'],
    ['documentos', 'Documentos'],
    ['extintor', 'Extintor'],
    ['triangulo', 'Triângulo'],
    ['macaco', 'Macaco'],
    ['estepe', 'Estepe'],
    ['limpeza', 'Limpeza']
  ] as const;
  readonly checklistForm;
  veiculoId: number | null = null;
  veiculo: Veiculo | null = null;
  veiculos: Veiculo[] = [];
  veiculoAutorizado = false;
  veiculoNaoAtribuido = false;
  erroVeiculos = false;
  checklists: Checklist[] = [];
  carregando = false;
  salvando = false;
  termoBusca = '';
  private buscaAgendada?: ReturnType<typeof setTimeout>;

  get ultimaInspecao(): Checklist | null {
    return this.checklists[0] || null;
  }

  get temPendencias(): boolean {
    const ultima = this.ultimaInspecao;
    if (!ultima) {
      return false;
    }

    return this.itens.some(([campo]) => ultima[campo] === false) || Boolean(ultima.observacoes?.trim());
  }

  constructor(
    private formBuilder: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private changeDetector: ChangeDetectorRef,
    dataRefresh: AppDataRefreshService,
    private authService: AuthService
  ) {
    this.checklistForm = this.formBuilder.group({
      quilometragem: [null, [Validators.min(0)]],
      pneus: [false],
      farois: [false],
      lanternas: [false],
      freios: [false],
      nivelOleo: [false],
      nivelAgua: [false],
      documentos: [false],
      extintor: [false],
      triangulo: [false],
      macaco: [false],
      estepe: [false],
      limpeza: [false],
      observacoes: ['']
    });

    this.route.queryParamMap.subscribe(params => {
      const id = params.get('veiculo');
      this.veiculoId = id ? Number(id) : null;
      this.veiculo = null;
      this.checklists = [];
      this.termoBusca = '';
      this.veiculoAutorizado = false;
      this.veiculoNaoAtribuido = false;
      this.erroVeiculos = false;

      if (this.isMotorista) {
        this.carregarVeiculosDoMotorista(this.veiculoId);
      } else if (this.veiculoId) {
        this.carregarVeiculo();
        this.carregarChecklists();
      } else {
        this.carregarVeiculos();
      }
    });

    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.isMotorista) {
        this.carregarVeiculosDoMotorista(this.veiculoId);
      } else if (this.veiculoId) {
        this.carregarVeiculo();
        this.carregarChecklists();
      } else {
        this.carregarVeiculos();
      }
    });
  }

  get isMotorista(): boolean {
    return this.authService.hasAnyRole('MOTORISTA') && !this.authService.isAdmin;
  }

  get isAssistenteFrota(): boolean {
    return this.authService.hasAnyRole('ASSISTENTE_FROTA') && !this.authService.isAdmin;
  }

  get rotaVolta(): string {
    return this.isMotorista ? '/minhas-rotas' : this.isAssistenteFrota ? '/assistente-frota' : '/veiculos';
  }

  carregarVeiculos(): void {
    this.carregando = true;
    this.http.get<Veiculo[]>('http://localhost:8080/api/veiculos').subscribe({
      next: veiculos => {
        this.veiculos = veiculos;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.changeDetector.markForCheck();
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar veículos',
          text: 'Não foi possível montar a seleção de veículos.'
        });
      }
    });
  }

  private carregarVeiculosDoMotorista(veiculoSolicitado: number | null): void {
    this.carregando = true;
    this.veiculos = [];
    this.erroVeiculos = false;
    this.http.get<RotaMotorista[]>('http://localhost:8080/api/rotas/minhas').subscribe({
      next: rotas => {
        this.veiculos = [...new Map(
          rotas.filter(rota => rota.veiculo.id !== undefined)
            .map(rota => [rota.veiculo.id!, rota.veiculo])
        ).values()];

        const selecionado = veiculoSolicitado === null
          ? undefined
          : this.veiculos.find(veiculo => veiculo.id === veiculoSolicitado);
        if (selecionado) {
          this.veiculoId = selecionado.id ?? null;
          this.veiculo = selecionado;
          this.veiculoAutorizado = true;
          this.carregarChecklists();
          return;
        }

        this.veiculoNaoAtribuido = veiculoSolicitado !== null;
        this.veiculoId = null;
        this.veiculo = null;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.veiculoId = null;
        this.veiculo = null;
        this.erroVeiculos = true;
        this.changeDetector.markForCheck();
      }
    });
  }

  recarregarVeiculosMotorista(): void {
    this.carregarVeiculosDoMotorista(this.veiculoId);
  }

  carregarVeiculo(): void {
    this.http.get<Veiculo>(`http://localhost:8080/api/veiculos/${this.veiculoId}`).subscribe({
      next: veiculo => {
        this.veiculo = veiculo;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar veículo',
          text: 'Não foi possível consultar os dados do veículo.'
        });
      }
    });
  }

  carregarChecklists(): void {
    this.carregando = true;
    const options = this.termoBusca.trim() ? { params: { busca: this.termoBusca.trim() } } : {};
    this.http.get<Checklist[]>(`http://localhost:8080/api/checklists/veiculo/${this.veiculoId}`, options).subscribe({
      next: checklists => {
        this.checklists = [...checklists].sort((a, b) =>
          new Date(b.dataHora || 0).getTime() - new Date(a.dataHora || 0).getTime()
        );
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.changeDetector.markForCheck();
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar',
          text: 'Não foi possível carregar os checklists deste veículo.'
        });
      }
    });
  }

  buscar(termo: string): void { this.termoBusca = termo; clearTimeout(this.buscaAgendada); this.buscaAgendada = setTimeout(() => this.carregarChecklists(), 300); }
  limparBusca(): void { if (this.termoBusca) this.buscar(''); }

  salvarChecklist(): void {
    if (!this.veiculoId) {
      Swal.fire({
        icon: 'warning',
        title: 'Veículo não selecionado',
        text: 'Abra o checklist a partir de um veículo da lista.'
      });
      return;
    }

    if (this.checklistForm.invalid) {
      this.checklistForm.markAllAsTouched();
      Swal.fire({
        icon: 'warning',
        title: 'Dados inválidos',
        text: 'Informe uma quilometragem válida.'
      });
      return;
    }

    this.salvando = true;
    this.http.post<Checklist>('http://localhost:8080/api/checklists', {
      ...this.checklistForm.getRawValue(),
      veiculoId: this.veiculoId
    }).pipe(
      timeout(10000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: checklist => {
        this.checklists = [checklist, ...this.checklists];
        this.checklistForm.reset({ quilometragem: null, ...Object.fromEntries(this.itens.map(([campo]) => [campo, false])), observacoes: '' });
        Swal.fire({
          icon: 'success',
          title: 'Checklist salvo',
          text: 'A inspeção do veículo foi registrada.',
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: () => {
        Swal.fire({
          icon: 'error',
          title: 'Erro ao salvar',
          text: 'Não foi possível registrar o checklist.'
        });
      }
    });
  }
}
