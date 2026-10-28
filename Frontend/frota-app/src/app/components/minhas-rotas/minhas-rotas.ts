import { isPlatformBrowser, CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, Inject, OnDestroy, OnInit, PLATFORM_ID, ViewChild, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import Swal from 'sweetalert2';
import type * as Leaflet from 'leaflet';
import { PontoParada, TipoParada } from '../../models/ponto-parada.model';
import { OfflineRegistroService } from '../../services/offline-registro.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

type StatusRota = 'PLANEJADA' | 'EM_ANDAMENTO' | 'FINALIZADA';
interface RegistroParada {
  id?: number;
  paradaId: number;
  tipo: 'CONCLUIDA' | 'OCORRENCIA';
  observacao: string | null;
  possuiFoto: boolean;
  registradoEm: string;
  clientRequestId?: string;
  estadoSincronizacao?: 'AGUARDANDO' | 'FALHOU';
  erroSincronizacao?: string;
}

interface RotaMotorista {
  rotaId: number | null;
  status: StatusRota | null;
  dataRota: string | null;
  finalizadaEm: string | null;
  veiculo: {
    id: number;
    placa: string;
    marca: string;
    modelo: string;
    frota?: string;
  };
  atribuidaEm: string;
  paradas: PontoParada[];
  registros: RegistroParada[];
}

interface EncaminhamentoManutencao {
  id: number;
  veiculo: RotaMotorista['veiculo'];
  oficina: {
    nomeOficina: string;
    telefone: string;
    endereco?: {
      logradouro: string;
      numero: string;
      bairro: string;
      cidade: string;
      estado: string;
      cep: string;
    };
  };
  orientacao?: string;
  enviadoEm: string;
}

@Component({
  selector: 'app-minhas-rotas',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink],
  templateUrl: './minhas-rotas.html',
  styleUrl: './minhas-rotas.css'
})
export class MinhasRotas implements OnInit, OnDestroy {
  rotas: RotaMotorista[] = [];
  encaminhamentos: EncaminhamentoManutencao[] = [];
  carregando = true;
  erro = false;
  erroEncaminhamentos = false;
  registrandoParadaId: number | null = null;
  finalizandoRotaId: number | null = null;
  rotaMapaId: string | null = null;
  private previaFotoUrl?: string;
  private rotaNoMapa?: RotaMotorista;
  private mapElement?: ElementRef<HTMLDivElement>;
  private leaflet?: typeof Leaflet;
  private map?: Leaflet.Map;
  private markersLayer?: Leaflet.FeatureGroup;
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('mapaRota')
  set mapaRotaView(element: ElementRef<HTMLDivElement> | undefined) {
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
    if (isPlatformBrowser(this.platformId)) {
      void this.inicializarMapa();
    }
  }

  constructor(
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef,
    readonly offline: OfflineRegistroService,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {
    this.offline.sincronizado$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregarRotas());
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.limparPreviaFoto();
  }

  ngOnInit(): void {
    void this.offline.iniciar().catch((erro: unknown) => {
      this.offline.erroArmazenamento.set(
        erro instanceof Error ? erro.message : 'Não foi possível abrir a fila offline.'
      );
    });
    this.carregarRotas();
  }

  carregarRotas(): void {
    this.carregando = true;
    this.erro = false;
    this.erroEncaminhamentos = false;
    this.carregarEncaminhamentos();
    this.http.get<RotaMotorista[]>('http://localhost:8080/api/rotas/minhas').pipe(
      timeout(10000),
      finalize(() => {
        this.carregando = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: rotas => {
        this.rotas = rotas;
        if (this.rotaMapaId) {
          this.rotaNoMapa = rotas.find(rota => this.idMapa(rota) === this.rotaMapaId);
          this.atualizarMapa();
        }
      },
      error: () => this.erro = true
    });
  }

  carregarEncaminhamentos(): void {
    this.http.get<EncaminhamentoManutencao[]>('http://localhost:8080/api/rotas/minhas/encaminhamentos').pipe(
      timeout(10000)
    ).subscribe({
      next: encaminhamentos => {
        this.encaminhamentos = encaminhamentos;
        this.erroEncaminhamentos = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.erroEncaminhamentos = true;
        this.changeDetector.markForCheck();
      }
    });
  }

  registrosDaParada(rota: RotaMotorista, parada: PontoParada): RegistroParada[] {
    const registrosServidor = rota.registros.filter(registro => registro.paradaId === parada.id);
    const registrosPendentes = this.offline.fila()
      .filter(registro => registro.paradaId === parada.id)
      .map(registro => ({
        paradaId: registro.paradaId,
        tipo: registro.tipo,
        observacao: registro.observacao,
        possuiFoto: true,
        registradoEm: registro.criadoEm,
        clientRequestId: registro.id,
        estadoSincronizacao: registro.estado,
        erroSincronizacao: registro.erro
      }));
    return [...registrosServidor, ...registrosPendentes]
      .sort((a, b) => Date.parse(b.registradoEm) - Date.parse(a.registradoEm));
  }

  private ultimoRegistroDaParada(rota: RotaMotorista, parada: PontoParada): RegistroParada | undefined {
    return this.registrosDaParada(rota, parada).reduce<RegistroParada | undefined>((maisRecente, atual) => {
      if (!maisRecente) return atual;
      const diferenca = Date.parse(atual.registradoEm) - Date.parse(maisRecente.registradoEm);
      return diferenca > 0 || (diferenca === 0 && (atual.id ?? 0) > (maisRecente.id ?? 0)) ? atual : maisRecente;
    }, undefined);
  }

  temRegistroPendente(rota: RotaMotorista, parada: PontoParada): boolean {
    return this.registrosDaParada(rota, parada).some(registro => registro.clientRequestId !== undefined);
  }

  registrosPendentesDaRota(rota: RotaMotorista): number {
    const paradaIds = new Set(rota.paradas.map(parada => parada.id));
    return this.offline.fila().filter(registro => paradaIds.has(registro.paradaId)).length;
  }

  tentarSincronizar(): void {
    void this.offline.sincronizar();
  }

  tentarRegistroNovamente(id: string): void {
    void this.offline.tentarNovamente(id);
  }

  paradaConcluida(rota: RotaMotorista, parada: PontoParada): boolean {
    return this.ultimoRegistroDaParada(rota, parada)?.tipo === 'CONCLUIDA';
  }

  rotuloConclusao(parada: PontoParada): string {
    return parada.tipo === 'ENTREGA' ? 'Entregue' : 'Retirada realizada';
  }

  idMapa(rota: RotaMotorista): string {
    return rota.rotaId === null ? `veiculo-${rota.veiculo.id}` : `rota-${rota.rotaId}`;
  }

  paradasComCoordenadas(rota: RotaMotorista): number {
    return rota.paradas.filter(parada => Number.isFinite(parada.latitude)
      && Number.isFinite(parada.longitude)
      && parada.latitude >= -90 && parada.latitude <= 90
      && parada.longitude >= -180 && parada.longitude <= 180).length;
  }

  alternarMapa(rota: RotaMotorista): void {
    const id = this.idMapa(rota);
    if (this.rotaMapaId === id) {
      this.rotaMapaId = null;
      this.rotaNoMapa = undefined;
      return;
    }
    this.rotaMapaId = id;
    this.rotaNoMapa = rota;
  }

  urlGoogleMaps(parada: PontoParada): string | null {
    const temCoordenadas = Number.isFinite(parada.latitude)
      && Number.isFinite(parada.longitude)
      && parada.latitude >= -90 && parada.latitude <= 90
      && parada.longitude >= -180 && parada.longitude <= 180;
    const destino = temCoordenadas
      ? `${parada.latitude},${parada.longitude}`
      : parada.endereco?.trim();
    return destino
      ? `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destino)}&travelmode=driving`
      : null;
  }

  urlGoogleMapsOficina(encaminhamento: EncaminhamentoManutencao): string | null {
    const endereco = encaminhamento.oficina.endereco;
    const destino = endereco
      ? `${endereco.logradouro}, ${endereco.numero}, ${endereco.cidade}, ${endereco.estado}`
      : '';
    return destino
      ? `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destino)}&travelmode=driving`
      : null;
  }

  private async inicializarMapa(): Promise<void> {
    const elemento = this.mapElement;
    if (!elemento || this.map) return;
    const leaflet = this.leaflet ?? await import('leaflet');
    this.leaflet = leaflet;
    if (!this.mapElement || this.mapElement !== elemento || this.map) return;
    this.map = leaflet.map(elemento.nativeElement, {
      center: [-14.235, -51.9253],
      zoom: 4,
      scrollWheelZoom: true
    });
    leaflet.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
    this.markersLayer = leaflet.featureGroup().addTo(this.map);
    this.atualizarMapa();
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private atualizarMapa(): void {
    if (!this.leaflet || !this.map || !this.markersLayer || !this.rotaNoMapa) return;
    const leaflet = this.leaflet;
    this.markersLayer.clearLayers();
    const paradas = this.rotaNoMapa.paradas;
    const coordenadas: Leaflet.LatLngExpression[] = [];

    paradas.forEach((parada, indice) => {
      if (!Number.isFinite(parada.latitude) || !Number.isFinite(parada.longitude)
        || parada.latitude < -90 || parada.latitude > 90
        || parada.longitude < -180 || parada.longitude > 180) return;
      const coordenada: Leaflet.LatLngExpression = [parada.latitude, parada.longitude];
      coordenadas.push(coordenada);
      const icone = leaflet.divIcon({
        className: `marcador-rota ${parada.tipo.toLowerCase()}`,
        html: `<span>${indice + 1}</span>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });
      const conteudo = document.createElement('div');
      const nome = document.createElement('strong');
      nome.textContent = `${indice + 1}. ${parada.tipo}: ${parada.nomeEstabelecimento || parada.descricao}`;
      conteudo.appendChild(nome);
      if (parada.endereco) {
        const endereco = document.createElement('div');
        endereco.textContent = parada.endereco;
        conteudo.appendChild(endereco);
      }
      leaflet.marker(coordenada, { icon: icone })
        .bindPopup(conteudo)
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

  podeFinalizar(rota: RotaMotorista): boolean {
    if (rota.status === 'FINALIZADA') return false;
    if (rota.rotaId === null) return true;
    return rota.dataRota === this.dataHoje();
  }

  podeRegistrar(rota: RotaMotorista): boolean {
    return (rota.dataRota === null || rota.dataRota === this.dataHoje())
      && rota.status !== 'FINALIZADA';
  }

  rotuloRota(rota: RotaMotorista): string {
    if (rota.status === 'FINALIZADA') {
      return `${rota.paradas.length - this.paradasPendentes(rota)} realizadas · ${this.paradasPendentes(rota)} não realizadas`;
    }
    if (rota.dataRota !== null && rota.dataRota > this.dataHoje()) return 'Programada';
    return `${rota.paradas.length} ${rota.paradas.length === 1 ? 'parada' : 'paradas'}`;
  }

  paradasPendentes(rota: RotaMotorista): number {
    return rota.paradas.filter(parada => !this.paradaConcluida(rota, parada)).length;
  }

  paradasPorTipo(rota: RotaMotorista, tipo: TipoParada): PontoParada[] {
    return rota.paradas.filter(parada => parada.tipo === tipo);
  }

  paradasConcluidasPorTipo(rota: RotaMotorista, tipo: TipoParada): PontoParada[] {
    return this.paradasPorTipo(rota, tipo).filter(parada => this.paradaConcluida(rota, parada));
  }

  estaFinalizando(rota: RotaMotorista): boolean {
    return this.finalizandoRotaId === (rota.rotaId ?? rota.veiculo.id);
  }

  finalizarDia(rota: RotaMotorista): void {
    if (!this.podeFinalizar(rota) || this.finalizandoRotaId !== null || this.registrosPendentesDaRota(rota) > 0) return;
    const pendentes = rota.paradas.filter(parada => !this.paradaConcluida(rota, parada)).length;
    Swal.fire({
      icon: 'question',
      title: 'Finalizar o dia?',
      text: pendentes
        ? `Há ${pendentes} ${pendentes === 1 ? 'parada pendente' : 'paradas pendentes'}. Elas aparecerão para o administrador como não realizadas.`
        : 'Todas as paradas foram concluídas.',
      showCancelButton: true,
      confirmButtonText: 'Finalizar dia',
      cancelButtonText: 'Continuar na rota'
    }).then(resultado => {
      if (!resultado.isConfirmed) return;
      const url = rota.rotaId === null
        ? `http://localhost:8080/api/rotas/veiculos/${rota.veiculo.id}/finalizar-dia`
        : `http://localhost:8080/api/rotas/diarias/${rota.rotaId}/finalizar`;
      this.finalizandoRotaId = rota.rotaId ?? rota.veiculo.id;
      this.http.post<RotaMotorista>(url, {}).pipe(
        timeout(10000),
        finalize(() => {
          this.finalizandoRotaId = null;
          this.changeDetector.markForCheck();
        })
      ).subscribe({
        next: rotaFinalizada => {
          this.rotas = this.rotas.map(atual =>
            atual.veiculo.id === rota.veiculo.id
              && (atual.rotaId === rota.rotaId || (atual.rotaId === null && rotaFinalizada.rotaId !== null))
              ? rotaFinalizada
              : atual
          );
          void Swal.fire({
            icon: 'success',
            title: 'Dia finalizado',
            text: pendentes
              ? 'O administrador já pode consultar as paradas realizadas e as pendentes.'
              : 'O administrador já pode preparar sua próxima rota.',
            timer: 2400,
            showConfirmButton: false
          });
        },
        error: () => void Swal.fire({
          icon: 'error',
          title: 'Não foi possível finalizar',
          text: 'Confira sua conexão e tente novamente.'
        })
      });
    });
  }

  confirmarConclusao(rota: RotaMotorista, parada: PontoParada): void {
    if (this.registrandoParadaId !== null || this.paradaConcluida(rota, parada)) return;
    Swal.fire<{ foto: File }>({
      icon: 'question',
      title: `Confirmar: ${this.rotuloConclusao(parada).toLowerCase()}?`,
      text: parada.nomeEstabelecimento || parada.descricao,
      html: this.htmlCapturaFoto(),
      showCancelButton: true,
      confirmButtonText: 'Confirmar',
      cancelButtonText: 'Voltar',
      didOpen: popup => this.prepararCapturaFoto(popup),
      willClose: () => this.limparPreviaFoto(),
      preConfirm: () => {
        const foto = this.obterFotoFormulario();
        return foto ? { foto } : undefined;
      }
    }).then(resultado => {
      if (resultado.isConfirmed && resultado.value) {
        this.salvarRegistro(
          rota,
          parada.id,
          'CONCLUIDA',
          undefined,
          resultado.value.foto
        );
      }
    });
  }

  registrarOcorrencia(rota: RotaMotorista, parada: PontoParada): void {
    if (this.registrandoParadaId !== null || this.paradaConcluida(rota, parada)) return;
    Swal.fire<{ foto: File; observacao: string }>({
      icon: 'warning',
      title: 'Registrar ocorrência',
      html: '<label class="relato-foto">Conte o que impediu a entrega ou retirada<textarea class="relato-ocorrencia" id="relato-ocorrencia" maxlength="1000" placeholder="Ex.: local fechado, destinatário ausente..."></textarea></label>' + this.htmlCapturaFoto(),
      showCancelButton: true,
      confirmButtonText: 'Salvar ocorrência',
      cancelButtonText: 'Cancelar',
      didOpen: popup => this.prepararCapturaFoto(popup),
      willClose: () => this.limparPreviaFoto(),
      preConfirm: () => {
        const observacao = document.getElementById('relato-ocorrencia') as HTMLTextAreaElement | null;
        const texto = observacao?.value.trim() ?? '';
        if (!texto) {
          Swal.showValidationMessage('Descreva o motivo da ocorrência.');
          return undefined;
        }
        const foto = this.obterFotoFormulario();
        return foto ? { foto, observacao: texto } : undefined;
      }
    }).then(resultado => {
      if (resultado.isConfirmed && resultado.value) {
        this.salvarRegistro(
          rota,
          parada.id,
          'OCORRENCIA',
          resultado.value.observacao,
          resultado.value.foto
        );
      }
    });
  }

  private htmlCapturaFoto(): string {
    return '<section class="captura-foto" aria-label="Foto obrigatória para comprovação">'
      + '<p>Foto obrigatória · JPEG ou PNG · até 8 MB</p>'
      + '<p>Data e hora do registro serão adicionadas à foto ao salvar.</p>'
      + '<input id="foto-parada" class="entrada-foto" type="file" accept="image/jpeg,image/png" capture="environment">'
      + '<button id="botao-tirar-foto" class="botao-tirar-foto" type="button">Tirar foto</button>'
      + '<img id="previa-foto-parada" class="previa-foto-parada" alt="Prévia da foto selecionada" hidden>'
      + '<span id="estado-foto-parada" class="estado-foto-parada">Nenhuma foto selecionada</span>'
      + '</section>';
  }

  private prepararCapturaFoto(popup: HTMLElement): void {
    const input = popup.querySelector<HTMLInputElement>('#foto-parada');
    const botao = popup.querySelector<HTMLButtonElement>('#botao-tirar-foto');
    const previa = popup.querySelector<HTMLImageElement>('#previa-foto-parada');
    const estado = popup.querySelector<HTMLElement>('#estado-foto-parada');
    if (!input || !botao || !previa || !estado) {
      throw new Error('Não foi possível preparar o controle da câmera para a foto.');
    }
    botao.addEventListener('click', () => input.click());
    input.addEventListener('change', () => {
      this.limparPreviaFoto();
      const foto = input.files?.[0];
      if (!foto) {
        previa.hidden = true;
        estado.textContent = 'Nenhuma foto selecionada';
        botao.textContent = 'Tirar foto';
        return;
      }
      this.previaFotoUrl = URL.createObjectURL(foto);
      previa.src = this.previaFotoUrl;
      previa.hidden = false;
      estado.textContent = foto.name;
      botao.textContent = 'Tirar outra foto';
    });
  }

  private limparPreviaFoto(): void {
    if (!this.previaFotoUrl) return;
    URL.revokeObjectURL(this.previaFotoUrl);
    this.previaFotoUrl = undefined;
  }

  private obterFotoFormulario(): File | null {
    const input = document.getElementById('foto-parada') as HTMLInputElement | null;
    const foto = input?.files?.[0];
    if (!foto) {
      Swal.showValidationMessage('Tire uma foto ou selecione uma imagem para continuar.');
      return null;
    }
    if (!['image/jpeg', 'image/png'].includes(foto.type)) {
      Swal.showValidationMessage('A foto deve estar no formato JPEG ou PNG.');
      return null;
    }
    if (foto.size > 8 * 1024 * 1024) {
      Swal.showValidationMessage('A foto deve ter no máximo 8 MB.');
      return null;
    }
    return foto;
  }

  private async salvarRegistro(
    rota: RotaMotorista,
    paradaId: number,
    tipo: RegistroParada['tipo'],
    observacao: string | undefined,
    foto: File
  ): Promise<void> {
    if (rota.status === 'FINALIZADA' || (rota.dataRota !== null && rota.dataRota !== this.dataHoje())) return;
    this.registrandoParadaId = paradaId;
    try {
      await this.offline.enfileirar(paradaId, tipo, observacao, foto);
      this.changeDetector.markForCheck();
      void Swal.fire({
        icon: 'success',
        title: tipo === 'CONCLUIDA' ? 'Parada salva' : 'Ocorrência salva',
        text: this.offline.conectado()
          ? 'Registro guardado neste aparelho; sincronização automática em andamento.'
          : 'Sem conexão. O registro e a foto serão enviados automaticamente quando a internet voltar.',
        timer: 3200,
        showConfirmButton: false
      });
    } catch (erro: unknown) {
      void Swal.fire({
        icon: 'error',
        title: 'Não foi possível guardar o registro',
        text: erro instanceof Error
          ? erro.message
          : 'O armazenamento offline falhou. A foto não foi enviada nem descartada.'
      });
    } finally {
      this.registrandoParadaId = null;
      this.changeDetector.markForCheck();
    }
  }

  private dataHoje(): string {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    const dia = String(hoje.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}
