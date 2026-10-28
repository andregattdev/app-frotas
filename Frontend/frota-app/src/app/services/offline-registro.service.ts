import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { firstValueFrom, Subject, timeout } from 'rxjs';
import { AuthService } from './auth.service';

export interface RegistroOffline {
  id: string;
  username: string;
  paradaId: number;
  tipo: 'CONCLUIDA' | 'OCORRENCIA';
  observacao: string | null;
  foto: Blob;
  nomeFoto: string;
  criadoEm: string;
  estado: 'AGUARDANDO' | 'FALHOU';
  erro?: string;
}

export interface RegistroParadaSincronizado {
  id: number;
  paradaId: number;
  tipo: 'CONCLUIDA' | 'OCORRENCIA';
  observacao: string | null;
  possuiFoto: boolean;
  registradoEm: string;
}

@Injectable({ providedIn: 'root' })
export class OfflineRegistroService {
  readonly fila = signal<RegistroOffline[]>([]);
  readonly conectado = signal(true);
  private readonly sincronizado = new Subject<void>();
  readonly sincronizado$ = this.sincronizado.asObservable();
  readonly erroArmazenamento = signal<string | null>(null);
  private readonly navegador: boolean;
  private sincronizando = false;
  private iniciado = false;

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.navegador = isPlatformBrowser(platformId);
    if (this.navegador) this.conectado.set(navigator.onLine);
  }

  async iniciar(): Promise<void> {
    if (!this.navegador) return;
    if (!this.iniciado) {
      this.iniciado = true;
      window.addEventListener('online', this.aoConectar);
      window.addEventListener('offline', this.aoDesconectar);
    }
    await this.atualizarFila();
    if (navigator.onLine) void this.sincronizar();
  }

  async tentarNovamente(id: string): Promise<void> {
    const registro = this.fila().find(item => item.id === id);
    if (!registro) return;
    await this.gravar({ ...registro, estado: 'AGUARDANDO', erro: undefined });
    await this.atualizarFila();
    if (navigator.onLine) void this.sincronizar();
  }

  async enfileirar(
    paradaId: number,
    tipo: RegistroOffline['tipo'],
    observacao: string | undefined,
    foto: File
  ): Promise<void> {
    const username = this.auth.username;
    if (!this.navegador || !username) {
      throw new Error('É necessário estar autenticado para guardar um registro offline.');
    }
    if (!['image/jpeg', 'image/png'].includes(foto.type) || foto.size > 8 * 1024 * 1024) {
      throw new Error('A foto deve ser JPEG ou PNG e ter no máximo 8 MB.');
    }

    const registro: RegistroOffline = {
      id: crypto.randomUUID(),
      username,
      paradaId,
      tipo,
      observacao: observacao?.trim() || null,
      foto,
      nomeFoto: foto.name || 'foto-parada',
      criadoEm: new Date().toISOString(),
      estado: 'AGUARDANDO'
    };

    await this.gravar(registro);
    await this.atualizarFila();
    if (navigator.onLine) void this.sincronizar();
  }

  async sincronizar(): Promise<void> {
    if (!this.navegador || this.sincronizando || !navigator.onLine || !this.auth.username) return;
    this.sincronizando = true;
    this.conectado.set(true);
    const tentados = new Set<string>();
    try {
      await this.atualizarFila();
      const username = this.auth.username;
      if (!username) return;
      const registros = this.fila().filter(registro =>
        registro.username === username && registro.estado === 'AGUARDANDO'
      );

      for (const registro of registros) {
        if (!navigator.onLine || this.auth.username !== username) break;
        tentados.add(registro.id);
        const dados = new FormData();
        dados.append('request', new Blob([JSON.stringify({
          tipo: registro.tipo,
          observacao: registro.observacao,
          clientRequestId: registro.id
        })], { type: 'application/json' }));
        dados.append('foto', registro.foto, registro.nomeFoto);

        try {
          await firstValueFrom(this.http.post<RegistroParadaSincronizado>(
            `http://localhost:8080/api/rotas/paradas/${registro.paradaId}/registros`,
            dados
          ).pipe(timeout(60000)));
          await this.remover(registro.id);
          this.sincronizado.next();
        } catch (erro: unknown) {
          const httpErro = erro instanceof HttpErrorResponse ? erro : null;
          if (httpErro && this.erroPermanente(httpErro.status)) {
            await this.gravar({
              ...registro,
              estado: 'FALHOU',
              erro: this.mensagemErro(httpErro)
            });
            await this.atualizarFila();
            continue;
          }

          this.conectado.set(navigator.onLine);
          break;
        }
      }
    } catch (erro: unknown) {
      this.erroArmazenamento.set(
        erro instanceof Error ? erro.message : 'Não foi possível acessar a fila offline.'
      );
    } finally {
      this.sincronizando = false;
      try {
        await this.atualizarFila();
        this.erroArmazenamento.set(null);
        if (navigator.onLine && this.fila().some(registro =>
          registro.estado === 'AGUARDANDO' && !tentados.has(registro.id)
        )) {
          void this.sincronizar();
        }
      } catch (erro: unknown) {
        this.erroArmazenamento.set(
          erro instanceof Error ? erro.message : 'Não foi possível atualizar a fila offline.'
        );
      }
    }
  }

  private readonly aoConectar = (): void => {
    this.conectado.set(true);
    void this.sincronizar();
  };

  private readonly aoDesconectar = (): void => {
    this.conectado.set(false);
  };

  private erroPermanente(status: number): boolean {
    return [400, 403, 404, 409, 413, 415, 422].includes(status);
  }

  private mensagemErro(erro: HttpErrorResponse): string {
    const mensagem = erro.error && typeof erro.error === 'object'
      ? erro.error.message
      : null;
    return typeof mensagem === 'string' && mensagem.trim()
      ? mensagem
      : 'O servidor recusou o registro. Os dados continuam salvos neste aparelho.';
  }

  private async atualizarFila(): Promise<void> {
    const username = this.auth.username;
    if (!this.navegador || !username) {
      this.fila.set([]);
      return;
    }
    const db = await this.abrirBanco();
    try {
      const registros = await new Promise<RegistroOffline[]>((resolve, reject) => {
        const request = db.transaction('registros', 'readonly').objectStore('registros').getAll();
        request.onsuccess = () => resolve((request.result as RegistroOffline[])
          .filter(registro => registro.username === username)
          .sort((a, b) => a.criadoEm.localeCompare(b.criadoEm)));
        request.onerror = () => reject(request.error ?? new Error('Não foi possível ler a fila offline.'));
      });
      this.fila.set(registros);
      this.erroArmazenamento.set(null);
    } finally {
      db.close();
    }
  }

  private async gravar(registro: RegistroOffline): Promise<void> {
    const db = await this.abrirBanco();
    try {
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction('registros', 'readwrite');
        transaction.objectStore('registros').put(registro);
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error ?? new Error('Não foi possível guardar o registro offline.'));
        transaction.onabort = () => reject(transaction.error ?? new Error('A gravação offline foi interrompida.'));
      });
    } finally {
      db.close();
    }
  }

  private async remover(id: string): Promise<void> {
    const db = await this.abrirBanco();
    try {
      await new Promise<void>((resolve, reject) => {
        const transaction = db.transaction('registros', 'readwrite');
        transaction.objectStore('registros').delete(id);
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error ?? new Error('Não foi possível confirmar a sincronização.'));
        transaction.onabort = () => reject(transaction.error ?? new Error('A atualização da fila foi interrompida.'));
      });
    } finally {
      db.close();
    }
  }

  private abrirBanco(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('frota-registros-offline', 1);
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains('registros')) {
          db.createObjectStore('registros', { keyPath: 'id' });
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error('Não foi possível abrir o armazenamento offline.'));
      request.onblocked = () => reject(new Error('Feche outras abas do Frota para atualizar o armazenamento offline.'));
    });
  }
}
