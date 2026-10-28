import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, OnInit, PLATFORM_ID, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { EMPTY, forkJoin, finalize, switchMap, timeout, timer } from 'rxjs';
import Swal from 'sweetalert2';
import type * as Leaflet from 'leaflet';

type TipoParada = 'ENTREGA' | 'RETIRADA';
type StatusRota = 'PLANEJADA' | 'EM_ANDAMENTO' | 'FINALIZADA';

interface Motorista {
  id: number;
  nome: string;
  cnh?: string;
}

interface Veiculo {
  id: number;
  placa: string;
  marca: string;
  modelo: string;
  frota?: string | null;
}

interface Parada {
  id: number;
  descricao: string;
  nomeEstabelecimento: string | null;
  endereco: string | null;
  tipo: TipoParada;
  latitude: number;
  longitude: number;
  ordem: number;
}

interface Registro {
  id: number;
  paradaId: number;
  tipo: 'CONCLUIDA' | 'OCORRENCIA';
  observacao: string | null;
  registradoEm: string;
}

interface RotaDiaria {
  id: number;
  dataRota: string;
  status: StatusRota;
  criadaEm: string;
  finalizadaEm: string | null;
  motoristaId: number;
  motoristaNome: string;
  veiculoId: number;
  veiculoPlaca: string;
  paradas: Parada[];
  registros: Registro[];
}

interface ParadaForm {
  id: number;
  cep: string;
  descricao: string;
  nomeEstabelecimento: string;
  endereco: string;
  numero: string;
  complemento: string;
  tipo: TipoParada;
  latitude: number | null;
  longitude: number | null;
}

interface ViaCepResposta {
  cep?: string;
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean;
}

interface ResultadoBuscaLocal {
  place_id: number;
  display_name: string;
  lat: string;
  lon: string;
  name?: string;
  class?: string;
  category?: string;
}

@Component({
  selector: 'app-rotas-diarias',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatIconModule],
  templateUrl: './rotas-diarias.html',
  styleUrl: './rotas-diarias.css'
})
export class RotasDiarias implements OnInit, OnDestroy {
  private mapElement?: ElementRef<HTMLDivElement>;

  @ViewChild('mapElement')
  set mapElementView(element: ElementRef<HTMLDivElement> | undefined) {
    this.mapElement = element;
    if (!element) {
      this.map?.remove();
      this.map = undefined;
      this.markersLayer = undefined;
      return;
    }
    if (isPlatformBrowser(this.platformId)) void this.inicializarMapa();
  }

  motoristas: Motorista[] = [];
  veiculos: Veiculo[] = [];
  rotas: RotaDiaria[] = [];
  rotaEmEdicaoId: number | null = null;
  motoristaId: number | null = null;
  veiculoId: number | null = null;
  dataRota = this.dataDeAmanha();
  paradas: ParadaForm[] = [];
  paradaAtual = this.novaParada();
  carregando = true;
  salvando = false;
  erro = false;
  erroFormulario = '';
  buscaLocal = '';
  resultadosBusca: ResultadoBuscaLocal[] = [];
  erroBuscaLocal = '';
  mensagemCoordenada = '';
  buscandoLocal = false;
  buscandoCep = false;
  identificandoCoordenada = false;

  private leaflet?: typeof Leaflet;
  private map?: Leaflet.Map;
  private markersLayer?: Leaflet.FeatureGroup;
  private proximoIdParada = 0;
  private requisicaoLocalId = 0;
  private ultimaBuscaLocal = 0;
  private ultimaGeocodificacao = 0;
  private idsParadasPersistidas = new Set<number>();

  constructor(
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  ngOnDestroy(): void {
    this.requisicaoLocalId++;
    this.map?.remove();
  }

  carregarDados(): void {
    this.carregando = true;
    this.erro = false;
    forkJoin({
      motoristas: this.http.get<Motorista[]>('http://localhost:8080/api/admin/motoristas'),
      veiculos: this.http.get<Veiculo[]>('http://localhost:8080/api/veiculos'),
      rotas: this.http.get<RotaDiaria[]>('http://localhost:8080/api/admin/rotas-diarias')
    }).pipe(
      timeout(15000),
      finalize(() => {
        this.carregando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: dados => {
        this.motoristas = dados.motoristas;
        this.veiculos = dados.veiculos;
        this.rotas = dados.rotas;
      },
      error: () => this.erro = true
    });
  }

  buscarCep(): void {
    const cep = this.paradaAtual.cep.replace(/\D/g, '');
    if (this.buscandoCep) return;
    if (cep.length !== 8) {
      this.erroBuscaLocal = 'Informe um CEP com 8 números.';
      return;
    }

    const requisicaoId = ++this.requisicaoLocalId;
    const numero = this.paradaAtual.numero.trim();
    const complemento = this.paradaAtual.complemento.trim();
    this.buscandoCep = true;
    this.erroBuscaLocal = '';
    this.http.get<ViaCepResposta>(`http://localhost:8080/api/enderecos/consulta-cep/${cep}`).pipe(
      timeout(8000),
      switchMap(resposta => {
        if (resposta.erro) throw new Error('CEP não encontrado. Confira o número informado.');
        const endereco = [
          [resposta.logradouro, numero].filter(Boolean).join(', '),
          complemento,
          resposta.bairro,
          resposta.localidade,
          resposta.uf,
          'Brasil'
        ].filter(valor => valor?.trim()).join(', ');
        this.paradaAtual.endereco = endereco;
        const espera = Math.max(0, 1000 - (Date.now() - this.ultimaGeocodificacao));
        return timer(espera).pipe(switchMap(() => {
          if (!this.consultaAindaAtual(requisicaoId, cep, numero, complemento)) {
            throw new Error('A consulta do CEP foi substituída.');
          }
          this.ultimaGeocodificacao = Date.now();
          return this.http.get<ResultadoBuscaLocal[]>('https://nominatim.openstreetmap.org/search', {
            params: {
              q: endereco,
              format: 'jsonv2',
              limit: '1',
              'accept-language': 'pt-BR'
            }
          }).pipe(timeout(10000));
        }));
      }),
      finalize(() => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.buscandoCep = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resultados => {
        if (!this.consultaAindaAtual(requisicaoId, cep, numero, complemento)) return;
        const resultado = resultados[0];
        if (!resultado) {
          this.erroBuscaLocal = 'Endereço preenchido, mas não foi possível encontrar as coordenadas. Informe-as pelo mapa ou manualmente.';
          return;
        }
        this.paradaAtual.latitude = Number(resultado.lat);
        this.paradaAtual.longitude = Number(resultado.lon);
        this.mensagemCoordenada = 'Endereço e coordenadas preenchidos pelo CEP. Confira a posição no mapa.';
        this.atualizarMapa();
      },
      error: erro => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.erroBuscaLocal = erro instanceof Error && erro.message.startsWith('CEP não encontrado')
          ? erro.message
          : 'Não foi possível localizar as coordenadas pelo endereço. Confira o CEP ou selecione o ponto no mapa.';
        this.changeDetector.markForCheck();
      }
    });
  }

  alterarBuscaLocal(consulta: string): void {
    this.requisicaoLocalId++;
    this.buscandoLocal = false;
    this.identificandoCoordenada = false;
    this.buscaLocal = consulta;
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
  }

  buscarLocal(): void {
    const consulta = this.buscaLocal.trim();
    if (!consulta || this.buscandoLocal) return;
    if (Date.now() - this.ultimaBuscaLocal < 1000) {
      this.erroBuscaLocal = 'Aguarde um instante antes de fazer outra busca.';
      return;
    }
    const requisicaoId = ++this.requisicaoLocalId;
    this.ultimaBuscaLocal = Date.now();
    this.buscandoLocal = true;
    this.erroBuscaLocal = '';
    this.resultadosBusca = [];
    this.http.get<ResultadoBuscaLocal[]>('https://nominatim.openstreetmap.org/search', {
      params: { q: consulta, format: 'jsonv2', limit: '5', 'accept-language': 'pt-BR' }
    }).pipe(
      timeout(10000),
      finalize(() => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.buscandoLocal = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resultados => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.resultadosBusca = resultados;
        if (resultados.length === 0) {
          this.erroBuscaLocal = 'Nenhum local encontrado. Tente informar mais detalhes, como cidade e estado.';
        }
      },
      error: () => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.erroBuscaLocal = 'Não foi possível pesquisar locais agora. Confira sua conexão e tente novamente.';
      }
    });
  }

  selecionarResultado(resultado: ResultadoBuscaLocal): void {
    this.requisicaoLocalId++;
    this.buscandoCep = false;
    this.buscandoLocal = false;
    const nomeAtual = this.paradaAtual.nomeEstabelecimento.trim();
    const localEhEstabelecimento = ['amenity', 'shop', 'tourism', 'office', 'craft', 'leisure', 'healthcare']
      .includes(resultado.class ?? resultado.category ?? '');
    this.paradaAtual.nomeEstabelecimento = nomeAtual || (localEhEstabelecimento ? resultado.name ?? '' : '');
    this.paradaAtual.endereco = resultado.display_name;
    this.paradaAtual.latitude = Number(resultado.lat);
    this.paradaAtual.longitude = Number(resultado.lon);
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.mensagemCoordenada = 'Local e coordenadas selecionados. Confira a posição no mapa.';
    this.atualizarMapa();
  }

  selecionarCoordenada(latitude: number, longitude: number): void {
    const requisicaoId = ++this.requisicaoLocalId;
    this.buscandoCep = false;
    this.buscandoLocal = false;
    this.identificandoCoordenada = false;
    this.paradaAtual.latitude = Number(latitude.toFixed(6));
    this.paradaAtual.longitude = Number(longitude.toFixed(6));
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.mensagemCoordenada = 'Buscando endereço para o ponto selecionado...';
    this.atualizarMapa();

    if (Date.now() - this.ultimaGeocodificacao < 1000) {
      this.mensagemCoordenada = 'Coordenadas preenchidas. Aguarde um instante; o endereço será localizado em seguida.';
      const espera = Math.max(0, 1000 - (Date.now() - this.ultimaGeocodificacao));
      timer(espera).pipe(switchMap(() => {
        if (requisicaoId !== this.requisicaoLocalId) return EMPTY;
        this.ultimaGeocodificacao = Date.now();
        return this.http.get<ResultadoBuscaLocal>('https://nominatim.openstreetmap.org/reverse', {
          params: {
            lat: latitude.toString(),
            lon: longitude.toString(),
            format: 'jsonv2',
            'accept-language': 'pt-BR',
            zoom: '18',
            addressdetails: '1'
          }
        }).pipe(timeout(10000));
      })).subscribe({
        next: resultado => {
          if (!resultado || requisicaoId !== this.requisicaoLocalId) return;
          this.paradaAtual.endereco = resultado.display_name ?? '';
          this.mensagemCoordenada = resultado.display_name
            ? 'Endereço e coordenadas preenchidos. Confira antes de adicionar a parada.'
            : 'Coordenadas preenchidas; não foi possível identificar o endereço.';
        },
        error: () => {
          if (requisicaoId === this.requisicaoLocalId) {
            this.mensagemCoordenada = 'Coordenadas preenchidas; não foi possível identificar o endereço. Você pode digitá-lo.';
          }
        }
      });
      return;
    }
    this.ultimaGeocodificacao = Date.now();
    this.identificandoCoordenada = true;
    this.http.get<ResultadoBuscaLocal>('https://nominatim.openstreetmap.org/reverse', {
      params: {
        lat: latitude.toString(),
        lon: longitude.toString(),
        format: 'jsonv2',
        'accept-language': 'pt-BR',
        zoom: '18',
        addressdetails: '1'
      }
    }).pipe(
      timeout(10000),
      finalize(() => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.identificandoCoordenada = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resultado => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.paradaAtual.endereco = resultado.display_name ?? '';
        this.mensagemCoordenada = resultado.display_name
          ? 'Endereço e coordenadas preenchidos. Confira antes de adicionar a parada.'
          : 'Coordenadas preenchidas; não foi possível identificar o endereço.';
      },
      error: () => {
        if (requisicaoId !== this.requisicaoLocalId) return;
        this.mensagemCoordenada = 'Coordenadas preenchidas; não foi possível identificar o endereço. Você pode digitá-lo.';
      }
    });
  }

  adicionarParada(): void {
    this.erroFormulario = '';
    const dados = this.validarParada(this.paradaAtual);
    if (!dados) return;
    const parada = { ...dados, id: this.paradaAtual.id };
    this.paradas = [...this.paradas, parada];
    this.paradaAtual = this.novaParada();
    this.limparBusca();
    this.atualizarMapa();
  }

  editarParada(parada: ParadaForm): void {
    if (this.paradaProtegida(parada)) return;
    this.paradaAtual = { ...parada };
    this.limparBusca();
    this.mensagemCoordenada = 'Parada selecionada. Edite os dados e salve as alterações.';
  }

  salvarEdicaoParada(): void {
    this.erroFormulario = '';
    const paradaEmEdicao = this.paradas.find(parada => parada.id === this.paradaAtual.id);
    if (paradaEmEdicao && this.paradaProtegida(paradaEmEdicao)) return;
    const dados = this.validarParada(this.paradaAtual);
    if (!dados) return;
    this.paradas = this.paradas.map(parada =>
      parada.id === this.paradaAtual.id ? { ...dados, id: parada.id } : parada
    );
    this.paradaAtual = this.novaParada();
    this.limparBusca();
    this.atualizarMapa();
  }

  excluirParada(parada: ParadaForm): void {
    if (this.rotaEmEdicaoId !== null) return;
    void Swal.fire({
      icon: 'warning',
      title: 'Remover parada?',
      text: `${parada.descricao || parada.nomeEstabelecimento || parada.endereco} será removida da sequência.`,
      showCancelButton: true,
      confirmButtonText: 'Remover',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c'
    }).then(resultado => {
      if (!resultado.isConfirmed) return;
      this.paradas = this.paradas.filter(item => item.id !== parada.id);
      if (this.paradaAtual.id === parada.id) {
        this.paradaAtual = this.novaParada();
        this.limparBusca();
      }
      this.atualizarMapa();
    });
  }

  moverParada(indice: number, deslocamento: -1 | 1): void {
    const destino = indice + deslocamento;
    if (destino < 0 || destino >= this.paradas.length) return;
    if (!this.podeMoverParada(indice, deslocamento)) return;
    const paradas = [...this.paradas];
    [paradas[indice], paradas[destino]] = [paradas[destino], paradas[indice]];
    this.paradas = paradas;
    this.atualizarMapa();
  }

  cancelarEdicao(): void {
    this.paradaAtual = this.novaParada();
    this.limparBusca();
    this.atualizarMapa();
  }

  editarRota(rota: RotaDiaria): void {
    if (rota.status === 'FINALIZADA') return;
    this.rotaEmEdicaoId = rota.id;
    this.motoristaId = rota.motoristaId;
    this.veiculoId = rota.veiculoId;
    this.dataRota = rota.dataRota;
    this.idsParadasPersistidas = new Set(rota.paradas.map(parada => parada.id));
    this.paradas = rota.paradas.map(parada => ({
      id: parada.id,
      cep: '',
      descricao: parada.descricao,
      nomeEstabelecimento: parada.nomeEstabelecimento ?? '',
      endereco: parada.endereco ?? '',
      numero: '',
      complemento: '',
      tipo: parada.tipo,
      latitude: parada.latitude,
      longitude: parada.longitude
    }));
    this.paradaAtual = this.novaParada();
    this.limparBusca();
    this.erroFormulario = '';
    this.atualizarMapa();
    setTimeout(() => document.querySelector('.rota-conteudo')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 0);
  }

  cancelarEdicaoRota(): void {
    this.rotaEmEdicaoId = null;
    this.idsParadasPersistidas.clear();
    this.motoristaId = null;
    this.veiculoId = null;
    this.dataRota = this.dataDeAmanha();
    this.paradas = [];
    this.paradaAtual = this.novaParada();
    this.erroFormulario = '';
    this.limparBusca();
    this.atualizarMapa();
  }

  paradaProtegida(parada: ParadaForm): boolean {
    return this.rotaEmEdicaoId !== null
      && this.idsParadasPersistidas.has(parada.id)
      && this.rotas.find(rota => rota.id === this.rotaEmEdicaoId)
        ?.registros.some(registro => registro.paradaId === parada.id) === true;
  }

  podeMoverParada(indice: number, deslocamento: -1 | 1): boolean {
    const destino = indice + deslocamento;
    return destino >= 0 && destino < this.paradas.length
      && !this.paradaProtegida(this.paradas[indice])
      && !this.paradaProtegida(this.paradas[destino]);
  }

  criarRota(): void {
    if (this.salvando) return;
    this.erroFormulario = '';
    if (this.carregando) return this.erroAoProgramar('Aguarde o carregamento dos motoristas e veículos.');
    if (this.motoristaId === null) return this.erroAoProgramar('Selecione um motorista para programar a rota.');
    if (this.veiculoId === null) return this.erroAoProgramar('Selecione um veículo para programar a rota.');
    if (!this.dataRota) return this.erroAoProgramar('Informe a data da rota.');
    if (this.dataRota < this.dataHoje()) return this.erroAoProgramar('A data da rota não pode estar no passado.');
    if (this.buscandoCep || this.buscandoLocal || this.identificandoCoordenada) {
      return this.erroAoProgramar('Aguarde a conclusão da busca de endereço antes de programar a rota.');
    }
    if (this.indiceParada(this.paradaAtual) >= 0) {
      return this.erroAoProgramar('Salve ou cancele a edição da parada antes de programar a rota.');
    }
    if (this.paradaAtualTemConteudo()) {
      return this.erroAoProgramar('Adicione a parada atual à sequência antes de programar a rota.');
    }
    if (this.paradas.length === 0) {
      return this.erroAoProgramar('Adicione pelo menos uma parada à sequência antes de programar a rota.');
    }

    if (this.rotaEmEdicaoId !== null) {
      this.atualizarRotaEmAndamento();
      return;
    }

    const request = {
      motoristaId: this.motoristaId,
      veiculoId: this.veiculoId,
      dataRota: this.dataRota,
      paradas: this.paradas.map(parada => ({
        descricao: parada.descricao.trim() || parada.nomeEstabelecimento.trim() || parada.endereco.trim().slice(0, 255),
        tipo: parada.tipo,
        latitude: parada.latitude,
        longitude: parada.longitude,
        nomeEstabelecimento: parada.nomeEstabelecimento.trim() || null,
        endereco: parada.endereco.trim() || null
      }))
    };

    this.salvando = true;
    this.http.post<RotaDiaria>('http://localhost:8080/api/admin/rotas-diarias', request).pipe(
      timeout(15000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: rota => {
        this.rotas = [rota, ...this.rotas];
        this.motoristaId = null;
        this.veiculoId = null;
        this.dataRota = this.dataDeAmanha();
        this.paradas = [];
        this.paradaAtual = this.novaParada();
        this.limparBusca();
        this.atualizarMapa();
        void Swal.fire({
          icon: 'success',
          title: 'Rota programada',
          text: `A rota de ${rota.motoristaNome} foi salva para ${this.formatarData(rota.dataRota)}.`,
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: (erro: HttpErrorResponse) => void Swal.fire({
        icon: 'error',
        title: 'Não foi possível programar a rota',
        text: this.mensagemErroApi(erro)
      })
    });
  }

  private atualizarRotaEmAndamento(): void {
    if (this.rotaEmEdicaoId === null || this.salvando) return;
    const rotaId = this.rotaEmEdicaoId;
    const request = {
      paradas: this.paradas.map(parada => ({
        id: this.idsParadasPersistidas.has(parada.id) ? parada.id : null,
        descricao: parada.descricao.trim() || parada.nomeEstabelecimento.trim() || parada.endereco.trim().slice(0, 255),
        tipo: parada.tipo,
        latitude: parada.latitude,
        longitude: parada.longitude,
        nomeEstabelecimento: parada.nomeEstabelecimento.trim() || null,
        endereco: parada.endereco.trim() || null
      }))
    };

    this.salvando = true;
    this.http.put<RotaDiaria>(`http://localhost:8080/api/admin/rotas-diarias/${rotaId}`, request).pipe(
      timeout(15000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: rotaAtualizada => {
        this.rotas = this.rotas.map(rota => rota.id === rotaAtualizada.id ? rotaAtualizada : rota);
        this.cancelarEdicaoRota();
        void Swal.fire({
          icon: 'success',
          title: 'Rota atualizada',
          text: 'As alterações já estão disponíveis para o motorista.',
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: (erro: HttpErrorResponse) => void Swal.fire({
        icon: 'error',
        title: 'Não foi possível atualizar a rota',
        text: this.mensagemErroApi(erro)
      })
    });
  }

  registrosDaParada(rota: RotaDiaria, parada: Parada): Registro[] {
    return rota.registros.filter(registro => registro.paradaId === parada.id);
  }

  private ultimoRegistroDaParada(rota: RotaDiaria, parada: Parada): Registro | undefined {
    return this.registrosDaParada(rota, parada).reduce<Registro | undefined>((maisRecente, atual) => {
      if (!maisRecente) return atual;
      const diferenca = Date.parse(atual.registradoEm) - Date.parse(maisRecente.registradoEm);
      return diferenca > 0 || (diferenca === 0 && atual.id > maisRecente.id) ? atual : maisRecente;
    }, undefined);
  }

  statusDaParada(rota: RotaDiaria, parada: Parada): string {
    const ultimoRegistro = this.ultimoRegistroDaParada(rota, parada);
    if (ultimoRegistro?.tipo === 'CONCLUIDA') return 'Realizada';
    if (ultimoRegistro?.tipo === 'OCORRENCIA') return 'Não realizada · ocorrência';
    return rota.status === 'FINALIZADA' ? 'Não realizada' : 'Pendente';
  }

  dataLabel(data: string): string {
    return this.formatarData(data);
  }

  dataHoje(): string {
    const hoje = new Date();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    const dia = String(hoje.getDate()).padStart(2, '0');
    return `${hoje.getFullYear()}-${mes}-${dia}`;
  }

  coordenadaValida(latitude: number | null, longitude: number | null): boolean {
    return latitude !== null && longitude !== null
      && Number.isFinite(latitude) && Number.isFinite(longitude)
      && latitude >= -90 && latitude <= 90
      && longitude >= -180 && longitude <= 180;
  }

  indiceParada(parada: ParadaForm): number {
    return this.paradas.findIndex(item => item.id === parada.id);
  }

  private validarParada(parada: ParadaForm): Omit<ParadaForm, 'id'> | null {
    if (!parada.descricao.trim() && !parada.nomeEstabelecimento.trim() && !parada.endereco.trim()) {
      this.erroFormulario = 'Informe uma descrição, estabelecimento ou endereço para identificar a parada.';
      return null;
    }
    if (!this.coordenadaValida(parada.latitude, parada.longitude)) {
      this.erroFormulario = 'Selecione o local no mapa, busque pelo CEP ou informe latitude e longitude válidas.';
      return null;
    }
    return {
      cep: parada.cep,
      descricao: parada.descricao.trim() || parada.nomeEstabelecimento.trim() || parada.endereco.trim().slice(0, 255),
      nomeEstabelecimento: parada.nomeEstabelecimento.trim(),
      endereco: parada.endereco.trim(),
      numero: parada.numero,
      complemento: parada.complemento,
      tipo: parada.tipo,
      latitude: parada.latitude,
      longitude: parada.longitude
    };
  }

  private paradaAtualTemConteudo(): boolean {
    return Boolean(
      this.paradaAtual.cep.trim()
      || this.paradaAtual.descricao.trim()
      || this.paradaAtual.nomeEstabelecimento.trim()
      || this.paradaAtual.endereco.trim()
      || this.paradaAtual.numero.trim()
      || this.paradaAtual.complemento.trim()
      || this.paradaAtual.latitude !== null
      || this.paradaAtual.longitude !== null
    );
  }

  private erroAoProgramar(mensagem: string): void {
    this.erroFormulario = mensagem;
    this.changeDetector.markForCheck();
    void Swal.fire({ icon: 'warning', title: 'Confira a rota', text: mensagem });
  }

  private limparBusca(): void {
    this.requisicaoLocalId++;
    this.buscaLocal = '';
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.mensagemCoordenada = '';
    this.buscandoCep = false;
    this.buscandoLocal = false;
    this.identificandoCoordenada = false;
  }

  private consultaAindaAtual(requisicaoId: number, cep: string, numero: string, complemento: string): boolean {
    return requisicaoId === this.requisicaoLocalId
      && this.paradaAtual.cep.replace(/\D/g, '') === cep
      && this.paradaAtual.numero.trim() === numero
      && this.paradaAtual.complemento.trim() === complemento;
  }

  private async inicializarMapa(): Promise<void> {
    if (!this.mapElement || this.map) return;
    const leaflet = this.leaflet ?? await import('leaflet');
    this.leaflet = leaflet;
    if (!this.mapElement || this.map) return;
    this.map = leaflet.map(this.mapElement.nativeElement, {
      center: [-14.235, -51.9253],
      zoom: 4,
      scrollWheelZoom: true
    });
    leaflet.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
    this.markersLayer = leaflet.featureGroup().addTo(this.map);
    this.map.on('click', evento => this.selecionarCoordenada(evento.latlng.lat, evento.latlng.lng));
    this.atualizarMapa();
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  atualizarMapa(): void {
    if (!this.leaflet || !this.map || !this.markersLayer) return;
    const leaflet = this.leaflet;
    this.markersLayer.clearLayers();
    const pontos = this.paradas.filter(parada => this.coordenadaValida(parada.latitude, parada.longitude));
    pontos.forEach((parada, indice) => {
      const marcador = leaflet.divIcon({
        className: `marcador-rota ${parada.tipo.toLowerCase()}`,
        html: `<span>${indice + 1}</span>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });
      const tooltip = document.createElement('span');
      tooltip.textContent = `${indice + 1}. ${parada.tipo}: ${parada.descricao}`;
      leaflet.marker([parada.latitude!, parada.longitude!], { icon: marcador })
        .bindTooltip(tooltip)
        .addTo(this.markersLayer!);
    });
    if (this.coordenadaValida(this.paradaAtual.latitude, this.paradaAtual.longitude)) {
      leaflet.marker([this.paradaAtual.latitude!, this.paradaAtual.longitude!])
        .bindTooltip('Local da parada em edição')
        .addTo(this.markersLayer);
    }

    const coordenadas = [
      ...pontos.map(parada => [parada.latitude!, parada.longitude!] as Leaflet.LatLngExpression),
      ...(this.coordenadaValida(this.paradaAtual.latitude, this.paradaAtual.longitude)
        ? [[this.paradaAtual.latitude!, this.paradaAtual.longitude!] as Leaflet.LatLngExpression]
        : [])
    ];
    if (coordenadas.length > 1) {
      leaflet.polyline(coordenadas, { color: '#1565c0', weight: 4, opacity: 0.8 }).addTo(this.markersLayer);
      this.map.fitBounds(leaflet.latLngBounds(coordenadas), { padding: [32, 32], maxZoom: 14 });
    } else if (coordenadas.length === 1) {
      this.map.setView(coordenadas[0], 14);
    }
  }

  private mensagemErroApi(erro: HttpErrorResponse): string {
    const corpo: unknown = erro.error;
    if (typeof corpo === 'object' && corpo !== null && 'message' in corpo) {
      const mensagem = (corpo as { message?: unknown }).message;
      if (typeof mensagem === 'string' && mensagem.trim()) return mensagem;
    }
    if (erro.status === 0) {
      return 'Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.';
    }
    return `A API recusou a solicitação (HTTP ${erro.status}). Confira os dados e se motorista ou veículo já possuem rota nessa data.`;
  }

  private novaParada(): ParadaForm {
    return {
      id: -(++this.proximoIdParada),
      cep: '',
      descricao: '',
      nomeEstabelecimento: '',
      endereco: '',
      numero: '',
      complemento: '',
      tipo: 'ENTREGA',
      latitude: null,
      longitude: null
    };
  }

  private dataDeAmanha(): string {
    const data = new Date();
    data.setDate(data.getDate() + 1);
    const deslocamento = data.getTimezoneOffset() * 60000;
    return new Date(data.getTime() - deslocamento).toISOString().slice(0, 10);
  }

  private formatarData(data: string): string {
    return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR');
  }
}
