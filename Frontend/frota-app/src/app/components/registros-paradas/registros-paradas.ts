import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { finalize, timeout } from 'rxjs';

type TipoRegistro = 'CONCLUIDA' | 'OCORRENCIA';

interface RegistroParada {
  id: number;
  paradaId: number;
  paradaDescricao: string;
  nomeEstabelecimento: string | null;
  endereco: string | null;
  tipoParada: 'ENTREGA' | 'RETIRADA';
  latitude: number;
  longitude: number;
  latitudeRegistro: number | null;
  longitudeRegistro: number | null;
  tipo: TipoRegistro;
  observacao: string | null;
  possuiFoto: boolean;
  motoristaId: number;
  motoristaNome: string;
  usuarioMotorista: string;
  veiculoId: number;
  veiculoPlaca: string;
  registradoEm: string;
}

@Component({
  selector: 'app-registros-paradas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatIconModule],
  templateUrl: './registros-paradas.html',
  styleUrl: './registros-paradas.css'
})
export class RegistrosParadas implements OnInit, OnDestroy {
  registros: RegistroParada[] = [];
  carregando = true;
  erro = false;
  termoBusca = '';
  filtro: 'TODOS' | TipoRegistro = 'TODOS';
  dataInicio = '';
  dataFim = '';
  motoristaFiltro = '';
  veiculoFiltro = '';
  fotos: Record<number, string> = {};
  fotosCarregando = new Set<number>();
  errosFoto = new Set<number>();
  private destruido = false;

  constructor(
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    this.carregarRegistros();
  }

  ngOnDestroy(): void {
    this.destruido = true;
    Object.values(this.fotos).forEach(url => URL.revokeObjectURL(url));
  }

  alternarFoto(registro: RegistroParada): void {
    if (this.fotos[registro.id]) {
      URL.revokeObjectURL(this.fotos[registro.id]);
      delete this.fotos[registro.id];
      this.changeDetector.markForCheck();
      return;
    }
    if (this.fotosCarregando.has(registro.id)) return;
    this.errosFoto.delete(registro.id);
    this.fotosCarregando.add(registro.id);
    this.http.get(`http://localhost:8080/api/admin/registros-paradas/${registro.id}/foto`, {
      responseType: 'blob'
    }).pipe(
      timeout(15000),
      finalize(() => {
        this.fotosCarregando.delete(registro.id);
        if (!this.destruido) this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: foto => {
        if (this.destruido) return;
        if (!foto.type.startsWith('image/')) {
          this.errosFoto.add(registro.id);
          return;
        }
        this.fotos[registro.id] = URL.createObjectURL(foto);
      },
      error: () => {
        if (!this.destruido) this.errosFoto.add(registro.id);
      }
    });
  }

  get registrosFiltrados(): RegistroParada[] {
    const termo = this.termoBusca.trim().toLocaleLowerCase('pt-BR');
    return this.registros.filter(registro => {
      if (this.filtro !== 'TODOS' && registro.tipo !== this.filtro) return false;
      const dataRegistro = registro.registradoEm.slice(0, 10);
      if (this.dataInicio && dataRegistro < this.dataInicio) return false;
      if (this.dataFim && dataRegistro > this.dataFim) return false;
      if (this.motoristaFiltro && String(registro.motoristaId) !== this.motoristaFiltro) return false;
      if (this.veiculoFiltro && String(registro.veiculoId) !== this.veiculoFiltro) return false;
      if (!termo) return true;
      return [
        registro.paradaDescricao,
        registro.nomeEstabelecimento,
        registro.endereco,
        registro.veiculoPlaca,
        registro.motoristaNome,
        registro.usuarioMotorista,
        registro.observacao,
        registro.latitudeRegistro,
        registro.longitudeRegistro
      ].some(valor => String(valor ?? '').toLocaleLowerCase('pt-BR').includes(termo));
    });
  }

  get motoristasDisponiveis(): { id: number; nome: string }[] {
    return [...new Map(this.registros.map(registro => [
      registro.motoristaId,
      { id: registro.motoristaId, nome: registro.motoristaNome }
    ])).values()].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }

  get veiculosDisponiveis(): { id: number; placa: string }[] {
    return [...new Map(this.registros.map(registro => [
      registro.veiculoId,
      { id: registro.veiculoId, placa: registro.veiculoPlaca }
    ])).values()].sort((a, b) => a.placa.localeCompare(b.placa, 'pt-BR'));
  }

  carregarRegistros(): void {
    this.carregando = true;
    this.erro = false;
    this.http.get<RegistroParada[]>('http://localhost:8080/api/admin/registros-paradas').pipe(
      timeout(10000),
      finalize(() => {
        this.carregando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: registros => this.registros = registros,
      error: () => this.erro = true
    });
  }

  tituloRegistro(registro: RegistroParada): string {
    if (registro.tipo === 'OCORRENCIA') return 'Ocorrência';
    return registro.tipoParada === 'ENTREGA' ? 'Entrega concluída' : 'Retirada concluída';
  }

  tituloParada(registro: RegistroParada): string {
    return registro.nomeEstabelecimento || registro.paradaDescricao;
  }

  exportarCsv(): void {
    if (!isPlatformBrowser(this.platformId) || this.registrosFiltrados.length === 0) return;
    const cabecalho = [
      'ID do registro', 'Data e hora', 'Motorista', 'Usuário', 'Veículo', 'Operação',
      'Resultado', 'Parada', 'Endereço da parada - latitude', 'Endereço da parada - longitude',
      'GPS da comprovação - latitude', 'GPS da comprovação - longitude', 'Relato', 'Foto disponível'
    ];
    const linhas = this.registrosFiltrados.map(registro => [
      registro.id,
      registro.registradoEm,
      registro.motoristaNome,
      registro.usuarioMotorista,
      registro.veiculoPlaca,
      registro.tipoParada === 'ENTREGA' ? 'Entrega' : 'Retirada',
      this.tituloRegistro(registro),
      this.tituloParada(registro),
      registro.endereco,
      registro.latitude,
      registro.longitude,
      registro.latitudeRegistro,
      registro.longitudeRegistro,
      registro.observacao,
      registro.possuiFoto ? 'Sim' : 'Não'
    ]);
    const csv = `\uFEFF${[cabecalho, ...linhas]
      .map(linha => linha.map(valor => this.campoCsv(valor)).join(','))
      .join('\r\n')}`;
    const arquivo = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(arquivo);
    const link = document.createElement('a');
    link.href = url;
    link.download = this.nomeArquivoCsv();
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  private campoCsv(valor: string | number | null): string {
    let texto = valor === null ? '' : String(valor);
    if (/^\s*[=+\-@]/.test(texto)) texto = `'${texto}`;
    return `"${texto.replace(/"/g, '""')}"`;
  }

  private nomeArquivoCsv(): string {
    const inicio = this.dataInicio || 'inicio';
    const fim = this.dataFim || 'hoje';
    return `historico-paradas-${inicio}-a-${fim}.csv`;
  }
}