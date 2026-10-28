import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';

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
}

interface Veiculo {
  modelo?: string;
  frota?: string;
}

@Component({
  imports: [CommonModule, MatIconModule, RouterLink],
  selector: 'app-manutencao-veiculo',
  styleUrl: './manutencao-veiculo.css',
  templateUrl: './manutencao-veiculo.html',
})
export class ManutencaoVeiculo {
  readonly camposChecklist: Array<[keyof Checklist, string]> = [
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
  ];
  readonly veiculoId: number | null;
  veiculo: Veiculo | null = null;
  checklist: Checklist | null = null;
  carregando = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private changeDetector: ChangeDetectorRef,
    dataRefresh: AppDataRefreshService
  ) {
    const id = this.route.snapshot.queryParamMap.get('veiculo');
    this.veiculoId = id ? Number(id) : null;

    if (this.veiculoId) {
      this.carregarVeiculo();
      this.carregarManutencao();
    }

    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.veiculoId) {
        this.carregarVeiculo();
        this.carregarManutencao();
      }
    });
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

  get itensPendentes(): string[] {
    if (!this.checklist) {
      return [];
    }

    return this.camposChecklist
      .filter(([campo]) => this.checklist?.[campo] === false)
      .map(([, nome]) => nome);
  }

  get temPendencias(): boolean {
    return this.itensPendentes.length > 0 || Boolean(this.checklist?.observacoes?.trim());
  }

  carregarManutencao(): void {
    this.carregando = true;
    this.http.get<Checklist[]>(`http://localhost:8080/api/checklists/veiculo/${this.veiculoId}`).pipe(
      timeout(10000)
    ).subscribe({
      next: checklists => {
        this.checklist = [...checklists].sort((a, b) =>
          new Date(b.dataHora || 0).getTime() - new Date(a.dataHora || 0).getTime()
        )[0] || null;
        this.carregando = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.changeDetector.markForCheck();
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar manutenção',
          text: 'Não foi possível consultar a última inspeção deste veículo.'
        });
      }
    });
  }
}
