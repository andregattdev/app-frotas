import { isPlatformBrowser, CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, OnInit, PLATFORM_ID, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { finalize, timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import type * as Leaflet from 'leaflet';
import { PontoParada, PontoParadaRequest, TipoParada } from '../../models/ponto-parada.model';

interface Veiculo {
  id: number;
  placa: string;
  modelo: string;
  marca: string;
  frota?: string;
}

interface Motorista {
  id: number;
  nome: string;
  cnh: string;
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

interface RegistroParada {
  id: number;
  paradaId: number;
  tipoParada: TipoParada;
  tipo: 'CONCLUIDA' | 'OCORRENCIA';
  observacao: string | null;
  motoristaNome: string;
  registradoEm: string;
}

@Component({
  selector: 'app-rota-veiculo',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './rota-veiculo.html',
  styleUrl: './rota-veiculo.css'
})
export class RotaVeiculo implements OnInit, OnDestroy {
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
    if (this.map && this.map.getContainer() !== element.nativeElement) {
      this.map.remove();
      this.map = undefined;
      this.markersLayer = undefined;
    }
    if (isPlatformBrowser(this.platformId) && this.veiculoId) {
      void this.inicializarMapa();
    }
  }

  readonly paradaForm: FormGroup;
  readonly tiposParada: TipoParada[] = ['ENTREGA', 'RETIRADA'];
  readonly veiculoId: number | null;
  veiculo?: Veiculo;
  motoristasComConta: Motorista[] = [];
  motoristasAtribuidos: Motorista[] = [];
  motoristaSelecionado: number | null = null;
  atribuindoMotorista = false;
  pontos: PontoParada[] = [];
  registrosParada: RegistroParada[] = [];
  buscaLocal = '';
  resultadosBusca: ResultadoBuscaLocal[] = [];
  erroBuscaLocal = '';
  buscandoLocal = false;
  identificandoCoordenada = false;
  mensagemCoordenada = '';
  carregando = true;
  salvando = false;
  reordenando = false;
  erroCarregamento = false;
  pontoEmEdicao?: number;

  private leaflet?: typeof Leaflet;
  private map?: Leaflet.Map;
  private markersLayer?: Leaflet.FeatureGroup;
  private ordemSalvaIds: number[] = [];
  private ultimaBuscaLocal = 0;
  private requisicaoEnderecoId = 0;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly http: HttpClient,
    private readonly route: ActivatedRoute,
    private readonly changeDetector: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {
    const id = Number(this.route.snapshot.queryParamMap.get('veiculo'));
    this.veiculoId = Number.isSafeInteger(id) && id > 0 ? id : null;
    this.paradaForm = this.formBuilder.group({
      descricao: ['', Validators.maxLength(255)],
      nomeEstabelecimento: ['', Validators.maxLength(255)],
      endereco: ['', Validators.maxLength(500)],
      tipo: ['ENTREGA', Validators.required],
      latitude: [null, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitude: [null, [Validators.required, Validators.min(-180), Validators.max(180)]]
    });
  }

  ngOnInit(): void {
    if (!this.veiculoId) {
      this.carregando = false;
      return;
    }
    this.carregarMotoristasComConta();
    this.carregarRota();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  get modoEdicao(): boolean {
    return this.pontoEmEdicao !== undefined;
  }

  get ordemAlterada(): boolean {
    return this.pontos.length !== this.ordemSalvaIds.length
      || this.pontos.some((ponto, indice) => this.ordemSalvaIds[indice] !== ponto.id);
  }

  registrosDaParada(paradaId: number): RegistroParada[] {
    return this.registrosParada.filter(registro => registro.paradaId === paradaId);
  }

  tituloRegistro(registro: RegistroParada): string {
    if (registro.tipo === 'OCORRENCIA') return 'Ocorrência';
    return registro.tipoParada === 'ENTREGA' ? 'Entrega concluída' : 'Retirada concluída';
  }

  carregarRota(): void {
    if (!this.veiculoId) return;
    this.carregando = true;
    this.erroCarregamento = false;
    forkJoin({
      veiculo: this.http.get<Veiculo>(`http://localhost:8080/api/veiculos/${this.veiculoId}`),
      pontos: this.http.get<PontoParada[]>(`http://localhost:8080/api/veiculos/${this.veiculoId}/paradas`),
      motoristas: this.http.get<Motorista[]>(`http://localhost:8080/api/veiculos/${this.veiculoId}/motoristas`),
      registros: this.http.get<RegistroParada[]>(`http://localhost:8080/api/admin/veiculos/${this.veiculoId}/registros-paradas`)
    }).pipe(
      timeout(10000),
      finalize(() => {
        this.carregando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resposta => {
        this.veiculo = resposta.veiculo;
        this.pontos = resposta.pontos;
        this.motoristasAtribuidos = resposta.motoristas;
        this.registrosParada = resposta.registros;
        this.ordemSalvaIds = resposta.pontos.map(ponto => ponto.id);
        this.atualizarMapa();
      },
      error: () => {
        this.erroCarregamento = true;
        Swal.fire({
          icon: 'error',
          title: 'Erro ao carregar rota',
          text: 'Não foi possível consultar o veículo e suas paradas.'
        });
      }
    });
  }

  carregarMotoristasComConta(): void {
    this.http.get<Motorista[]>('http://localhost:8080/api/admin/motoristas').pipe(timeout(10000)).subscribe({
      next: motoristas => {
        this.motoristasComConta = motoristas;
        this.changeDetector.markForCheck();
      },
      error: () => Swal.fire({
        icon: 'error',
        title: 'Não foi possível carregar os motoristas',
        text: 'Verifique sua sessão de administrador e tente novamente.'
      })
    });
  }

  atribuirMotorista(): void {
    if (!this.veiculoId || !this.motoristaSelecionado || this.atribuindoMotorista) return;
    this.atribuindoMotorista = true;
    this.http.post<Veiculo>(`http://localhost:8080/api/admin/veiculos/${this.veiculoId}/motoristas/${this.motoristaSelecionado}`, null)
      .pipe(timeout(10000), finalize(() => {
        this.atribuindoMotorista = false;
        this.changeDetector.markForCheck();
      }))
      .subscribe({
        next: () => {
          const motorista = this.motoristasComConta.find(item => item.id === this.motoristaSelecionado);
          this.motoristasAtribuidos = motorista ? [motorista] : [];
          this.motoristaSelecionado = null;
          Swal.fire({ icon: 'success', title: 'Rota atribuída', text: 'O motorista poderá consultar esta rota em Minhas rotas.', timer: 2200 });
        },
        error: () => Swal.fire({
          icon: 'error',
          title: 'Não foi possível atribuir a rota',
          text: 'Confira se o motorista possui uma conta ativa e tente novamente.'
        })
      });
  }

  iniciarNovaParada(): void {
    this.pontoEmEdicao = undefined;
    this.paradaForm.reset({
      descricao: '',
      nomeEstabelecimento: '',
      endereco: '',
      tipo: 'ENTREGA',
      latitude: null,
      longitude: null
    });
    this.paradaForm.markAsPristine();
    this.buscaLocal = '';
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.identificandoCoordenada = false;
    this.mensagemCoordenada = '';
    this.requisicaoEnderecoId++;
  }

  editarParada(ponto: PontoParada): void {
    if (this.ordemAlterada) return;
    this.pontoEmEdicao = ponto.id;
    this.paradaForm.reset({
      descricao: ponto.descricao,
      nomeEstabelecimento: ponto.nomeEstabelecimento ?? '',
      endereco: ponto.endereco ?? '',
      tipo: ponto.tipo,
      latitude: ponto.latitude,
      longitude: ponto.longitude
    });
    this.paradaForm.markAsPristine();
    this.buscaLocal = '';
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.identificandoCoordenada = false;
    this.mensagemCoordenada = '';
    this.requisicaoEnderecoId++;
  }

  alterarBuscaLocal(consulta: string): void {
    this.requisicaoEnderecoId++;
    this.buscaLocal = consulta;
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.identificandoCoordenada = false;
    this.mensagemCoordenada = '';
    this.paradaForm.patchValue({ endereco: '', latitude: null, longitude: null });
  }

  buscarLocal(): void {
    const consulta = this.buscaLocal.trim();
    if (!consulta || this.buscandoLocal) return;
    if (Date.now() - this.ultimaBuscaLocal < 1000) {
      this.erroBuscaLocal = 'Aguarde um instante antes de fazer outra busca.';
      return;
    }

    this.ultimaBuscaLocal = Date.now();
    this.buscandoLocal = true;
    this.erroBuscaLocal = '';
    this.resultadosBusca = [];
    this.http.get<ResultadoBuscaLocal[]>('https://nominatim.openstreetmap.org/search', {
      params: {
        q: consulta,
        format: 'jsonv2',
        limit: '5',
        'accept-language': 'pt-BR'
      }
    }).pipe(
      timeout(10000),
      finalize(() => {
        this.buscandoLocal = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resultados => {
        this.resultadosBusca = resultados;
        if (resultados.length === 0) {
          this.erroBuscaLocal = 'Nenhum local encontrado. Tente informar mais detalhes, como cidade e estado.';
        }
      },
      error: () => {
        this.erroBuscaLocal = 'Não foi possível pesquisar locais agora. Confira sua conexão e tente novamente.';
      }
    });
  }

  selecionarResultado(resultado: ResultadoBuscaLocal): void {
    this.requisicaoEnderecoId++;
    this.identificandoCoordenada = false;
    const nomeAtual = this.paradaForm.controls['nomeEstabelecimento'].value as string;
    const estabelecimentoOsm = this.ehEstabelecimentoOsm(resultado);
    this.paradaForm.patchValue({
      nomeEstabelecimento: nomeAtual.trim() || (estabelecimentoOsm ? resultado.name ?? '' : ''),
      endereco: resultado.display_name,
      latitude: Number(resultado.lat),
      longitude: Number(resultado.lon)
    });
    this.paradaForm.markAsDirty();
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    this.mensagemCoordenada = 'Confira o local selecionado. A parada só será registrada ao clicar em salvar.';
    this.changeDetector.markForCheck();
  }

  selecionarCoordenada(latitude: number, longitude: number): void {
    if (!this.paradaForm) return;
    const requisicaoId = ++this.requisicaoEnderecoId;
    this.paradaForm.patchValue({
      latitude: Number(latitude.toFixed(6)),
      longitude: Number(longitude.toFixed(6)),
      endereco: '',
      nomeEstabelecimento: ''
    });
    this.paradaForm.markAsDirty();
    this.resultadosBusca = [];
    this.erroBuscaLocal = '';
    if (Date.now() - this.ultimaBuscaLocal < 1000) {
      this.identificandoCoordenada = false;
      this.mensagemCoordenada = 'As coordenadas foram preenchidas. Aguarde um instante antes de identificar o endereço, ou informe-o manualmente.';
      this.changeDetector.markForCheck();
      return;
    }

    this.ultimaBuscaLocal = Date.now();
    this.identificandoCoordenada = true;
    this.mensagemCoordenada = 'Buscando endereço e estabelecimento próximos do ponto selecionado...';
    this.changeDetector.markForCheck();

    this.http.get<ResultadoBuscaLocal>('https://nominatim.openstreetmap.org/reverse', {
      params: {
        lat: latitude.toString(),
        lon: longitude.toString(),
        format: 'jsonv2',
        'accept-language': 'pt-BR',
        zoom: '18',
        addressdetails: '1'
      }
    }).pipe(timeout(10000)).subscribe({
      next: resultado => {
        if (requisicaoId !== this.requisicaoEnderecoId) return;
        const nomeAtual = this.paradaForm.controls['nomeEstabelecimento'].value as string;
        const estabelecimentoOsm = this.ehEstabelecimentoOsm(resultado);
        this.paradaForm.patchValue({
          nomeEstabelecimento: nomeAtual.trim() || (estabelecimentoOsm ? resultado.name ?? '' : ''),
          endereco: resultado.display_name ?? ''
        });
        this.mensagemCoordenada = resultado.display_name
          ? 'Confira o endereço e os dados preenchidos. A parada só será registrada ao clicar em salvar.'
          : 'O ponto foi selecionado, mas não foi possível identificar um endereço próximo. Você pode preencher o endereço manualmente.';
        this.identificandoCoordenada = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        if (requisicaoId !== this.requisicaoEnderecoId) return;
        this.identificandoCoordenada = false;
        this.mensagemCoordenada = 'Não foi possível identificar o endereço. As coordenadas foram preenchidas; informe o endereço manualmente ou tente outro ponto.';
        this.changeDetector.markForCheck();
      }
    });
  }

  salvarParada(): void {
    if (!this.veiculoId || this.identificandoCoordenada || this.ordemAlterada) return;
    if (this.identificandoCoordenada) return;
    if (this.paradaForm.invalid) {
      this.paradaForm.markAllAsTouched();
      Swal.fire({
        icon: 'warning',
        title: 'Dados da parada incompletos',
        text: 'Informe a descrição, o tipo e coordenadas válidas. Você também pode escolher as coordenadas clicando no mapa.'
      });
      return;
    }

    const valorFormulario = this.paradaForm.getRawValue();
    const nomeEstabelecimento = String(valorFormulario['nomeEstabelecimento'] ?? '').trim() || null;
    const endereco = String(valorFormulario['endereco'] ?? '').trim() || null;
    const descricaoDigitada = String(valorFormulario['descricao'] ?? '').trim();
    if (!descricaoDigitada && !nomeEstabelecimento && !endereco) {
      Swal.fire({
        icon: 'warning',
        title: 'Identifique a parada',
        text: 'Informe uma descrição, um estabelecimento ou um endereço para identificar esta parada.'
      });
      return;
    }
    const dados: PontoParadaRequest = {
      descricao: descricaoDigitada || nomeEstabelecimento || (endereco ?? '').slice(0, 255),
      nomeEstabelecimento,
      endereco,
      tipo: valorFormulario['tipo'] as TipoParada,
      latitude: Number(valorFormulario['latitude']),
      longitude: Number(valorFormulario['longitude'])
    };
    const url = `http://localhost:8080/api/veiculos/${this.veiculoId}/paradas`;
    const estavaEditando = this.modoEdicao;
    const requisicao = estavaEditando
      ? this.http.put<PontoParada>(`${url}/${this.pontoEmEdicao}`, dados)
      : this.http.post<PontoParada>(url, dados);

    this.salvando = true;
    requisicao.pipe(
      timeout(10000),
      finalize(() => {
        this.salvando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.iniciarNovaParada();
        this.carregarRota();
        Swal.fire({
          icon: 'success',
          title: estavaEditando ? 'Parada atualizada' : 'Parada adicionada',
          timer: 1600,
          showConfirmButton: false
        });
      },
      error: () => Swal.fire({
        icon: 'error',
        title: 'Erro ao salvar parada',
        text: 'Não foi possível salvar os dados da parada.'
      })
    });
  }

  excluirParada(ponto: PontoParada): void {
    if (!this.veiculoId || this.ordemAlterada) return;
    Swal.fire({
      icon: 'warning',
      title: 'Remover parada?',
      text: `${ponto.descricao} será removida da rota.`,
      showCancelButton: true,
      confirmButtonText: 'Remover',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c'
    }).then(resultado => {
      if (!resultado.isConfirmed) return;
      this.http.delete<void>(
        `http://localhost:8080/api/veiculos/${this.veiculoId}/paradas/${ponto.id}`
      ).pipe(timeout(10000)).subscribe({
        next: () => {
          if (this.pontoEmEdicao === ponto.id) this.iniciarNovaParada();
          this.carregarRota();
        },
        error: () => Swal.fire({
          icon: 'error',
          title: 'Erro ao remover parada',
          text: 'Não foi possível remover esta parada da rota.'
        })
      });
    });
  }

  moverParada(indice: number, deslocamento: -1 | 1): void {
    if (this.reordenando) return;
    const destino = indice + deslocamento;
    if (destino < 0 || destino >= this.pontos.length) return;

    const pontos = [...this.pontos];
    [pontos[indice], pontos[destino]] = [pontos[destino], pontos[indice]];
    this.pontos = pontos;
    this.atualizarMapa();
  }

  salvarSequencia(): void {
    if (!this.veiculoId || !this.ordemAlterada || this.reordenando) return;
    this.reordenando = true;
    this.http.put<PontoParada[]>(
      `http://localhost:8080/api/veiculos/${this.veiculoId}/paradas/ordem`,
      this.pontos.map(ponto => ponto.id)
    ).pipe(
      timeout(10000),
      finalize(() => {
        this.reordenando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: pontos => {
        this.pontos = pontos;
        this.ordemSalvaIds = pontos.map(ponto => ponto.id);
        this.atualizarMapa();
        Swal.fire({
          icon: 'success',
          title: 'Sequência salva',
          text: 'A ordem das paradas foi registrada.',
          timer: 1800,
          showConfirmButton: false
        });
      },
      error: () => Swal.fire({
        icon: 'error',
        title: 'Erro ao reordenar rota',
        text: 'Não foi possível atualizar a sequência das paradas.'
      })
    });
  }

  cancelarAlteracoesOrdem(): void {
    if (!this.ordemAlterada || this.reordenando) return;
    const ordemSalva = new Map(this.ordemSalvaIds.map((id, indice) => [id, indice]));
    this.pontos = [...this.pontos].sort(
      (primeiro, segundo) => (ordemSalva.get(primeiro.id) ?? 0) - (ordemSalva.get(segundo.id) ?? 0)
    );
    this.atualizarMapa();
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
    this.map.on('click', evento => {
      this.selecionarCoordenada(evento.latlng.lat, evento.latlng.lng);
    });
    this.atualizarMapa();
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private ehEstabelecimentoOsm(resultado: ResultadoBuscaLocal): boolean {
    return ['amenity', 'shop', 'tourism', 'office', 'craft', 'leisure', 'healthcare']
      .includes(resultado.class ?? resultado.category ?? '');
  }

  private atualizarMapa(): void {
    if (!this.leaflet || !this.map || !this.markersLayer) return;
    const leaflet = this.leaflet;
    this.markersLayer.clearLayers();
    const coordenadas = this.pontos.map(
      ponto => [ponto.latitude, ponto.longitude] as Leaflet.LatLngExpression
    );

    this.pontos.forEach((ponto, indice) => {
      const icone = leaflet.divIcon({
        className: `marcador-rota ${ponto.tipo.toLowerCase()}`,
        html: `<span>${indice + 1}</span>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });
      const tooltip = document.createElement('span');
      const nomeLocal = ponto.nomeEstabelecimento ? ` · ${ponto.nomeEstabelecimento}` : '';
      const endereco = ponto.endereco ? ` · ${ponto.endereco}` : '';
      tooltip.textContent = `${indice + 1}. ${ponto.tipo}: ${ponto.descricao}${nomeLocal}${endereco}`;
      leaflet.marker([ponto.latitude, ponto.longitude], { icon: icone })
        .bindTooltip(tooltip)
        .addTo(this.markersLayer!);
    });

    if (coordenadas.length > 1) {
      leaflet.polyline(coordenadas, { color: '#1565c0', weight: 4, opacity: 0.8 })
        .addTo(this.markersLayer);
      this.map.fitBounds(leaflet.latLngBounds(coordenadas), { padding: [32, 32], maxZoom: 14 });
    } else if (coordenadas.length === 1) {
      this.map.setView(coordenadas[0], 14);
    } else {
      this.map.setView([-14.235, -51.9253], 4);
    }
  }
}
