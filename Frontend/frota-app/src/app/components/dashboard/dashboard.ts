import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';

interface Veiculo { id?: number; placa: string; modelo: string; marca: string; frota?: string; }
interface Motorista { id?: number; nome: string; }
interface Endereco { id?: number; }
interface Checklist {
  id?: number;
  dataHora?: string;
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
  veiculo?: { id?: number };
}

interface AlertaChecklist {
  veiculo: Veiculo;
  motivo: string;
}

@Component({
  imports: [CommonModule, RouterLink, MatIconModule],
  selector: 'app-dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly itensChecklist: [keyof Checklist, string][] = [
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
    ['limpeza', 'Limpeza'],
  ];

  veiculos: Veiculo[] = [];
  motoristas: Motorista[] = [];
  enderecos: Endereco[] = [];
  checklists: Checklist[] = [];
  carregando = true;
  erroCarregamento = false;

  constructor(private http: HttpClient, private changeDetector: ChangeDetectorRef, dataRefresh: AppDataRefreshService) {
    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => this.carregarResumo());
  }

  ngOnInit(): void {
    this.carregarResumo();
  }

  get alertas(): string[] {
    const alertas: string[] = [];
    if (this.veiculos.length === 0) alertas.push('Cadastre o primeiro veículo para iniciar o controle da frota.');
    if (this.motoristas.length === 0) alertas.push('Cadastre motoristas para organizar a operação.');
    return alertas;
  }

  get veiculosAtencaoChecklist(): AlertaChecklist[] {
    const ultimoPorVeiculo = new Map<number, Checklist>();
    for (const checklist of this.checklists) {
      const veiculoId = checklist.veiculo?.id;
      if (veiculoId === undefined) continue;

      const atual = ultimoPorVeiculo.get(veiculoId);
      if (!atual || Date.parse(checklist.dataHora || '') >= Date.parse(atual.dataHora || '')) {
        ultimoPorVeiculo.set(veiculoId, checklist);
      }
    }

    return this.veiculos.flatMap(veiculo => {
      if (veiculo.id === undefined) return [];

      const ultimoChecklist = ultimoPorVeiculo.get(veiculo.id);
      if (!ultimoChecklist) return [{ veiculo, motivo: 'Checklist ainda não realizado' }];

      const problemas = this.itensChecklist
        .filter(([campo]) => ultimoChecklist[campo] === false)
        .map(([, nome]) => nome);
      if (ultimoChecklist.observacoes?.trim()) problemas.push('Há observações registradas');

      return problemas.length > 0 ? [{ veiculo, motivo: problemas.join(', ') }] : [];
    });
  }

  carregarResumo(): void {
    this.carregando = true;
    this.erroCarregamento = false;
    forkJoin({
      veiculos: this.http.get<Veiculo[]>('http://localhost:8080/api/veiculos').pipe(
        catchError(() => { this.erroCarregamento = true; return of([] as Veiculo[]); })
      ),
      motoristas: this.http.get<Motorista[]>('http://localhost:8080/api/motoristas').pipe(
        catchError(() => { this.erroCarregamento = true; return of([] as Motorista[]); })
      ),
      enderecos: this.http.get<Endereco[]>('http://localhost:8080/api/enderecos').pipe(
        catchError(() => { this.erroCarregamento = true; return of([] as Endereco[]); })
      ),
      checklists: this.http.get<Checklist[]>('http://localhost:8080/api/checklists').pipe(
        catchError(() => { this.erroCarregamento = true; return of([] as Checklist[]); })
      )
    }).subscribe({
      next: resumo => {
        this.veiculos = resumo.veiculos;
        this.motoristas = resumo.motoristas;
        this.enderecos = resumo.enderecos;
        this.checklists = resumo.checklists;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.erroCarregamento = true;
        this.changeDetector.markForCheck();
      }
    });
  }
}
