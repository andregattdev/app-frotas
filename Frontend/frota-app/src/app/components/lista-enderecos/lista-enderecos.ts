import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';
import { PagedResponse } from '../../services/paged-response';
import { AuthService } from '../../services/auth.service';

interface Endereco {
  id?: number;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  estado: string;
  cep: string;
}

interface Oficina {
  id?: number;
  nomeOficina: string;
  telefone: string;
  especialidade: string;
  endereco?: Endereco;
}

interface ViaCepResposta {
  cep?: string;
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean;
}

@Component({
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, RouterLink],
  selector: 'app-lista-enderecos',
  styleUrl: './lista-enderecos.css',
  templateUrl: './lista-enderecos.html',
})
export class ListaEnderecos implements OnInit {
  abaAtiva: 'oficinas' | 'enderecos' = 'oficinas';

  // --- OFICINAS ---
  readonly oficinaForm: FormGroup;
  oficinas: Oficina[] = [];
  modalOficinaAberto = false;
  carregandoOficinas = false;
  salvandoOficina = false;
  idOficinaEmEdicao?: number;
  paginaAtualOficina = 1;
  readonly itensPorPagina = 5;
  totalPaginasOficina = 1;
  totalOficinas = 0;
  buscandoCepOficina = false;
  termoBuscaOficina = '';
  private buscaAgendadaOficina?: ReturnType<typeof setTimeout>;

  // --- ENDEREÇOS ---
  readonly enderecoForm: FormGroup;
  enderecos: Endereco[] = [];
  modalEnderecoAberto = false;
  carregandoEnderecos = false;
  salvandoEndereco = false;
  idEnderecoEmEdicao?: number;
  paginaAtualEndereco = 1;
  totalPaginasEndereco = 1;
  totalEnderecos = 0;
  buscandoCepEndereco = false;
  termoBuscaEndereco = '';
  private buscaAgendadaEndereco?: ReturnType<typeof setTimeout>;

  get isAssistenteFrota(): boolean {
    return this.authService.hasAnyRole('ASSISTENTE_FROTA') && !this.authService.isAdmin;
  }

  get oficinasPaginadas(): Oficina[] {
    return this.oficinas;
  }

  get enderecosPaginados(): Endereco[] {
    return this.enderecos;
  }

  constructor(
    private formBuilder: FormBuilder,
    private http: HttpClient,
    private changeDetector: ChangeDetectorRef,
    dataRefresh: AppDataRefreshService,
    private authService: AuthService
  ) {
    if (this.isAssistenteFrota) this.abaAtiva = 'oficinas';
    this.oficinaForm = this.formBuilder.group({
      nomeOficina: ['', Validators.required],
      telefone: ['', Validators.required],
      especialidade: ['', Validators.required],
      cep: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(9)]],
      logradouro: ['', Validators.required],
      numero: ['', Validators.required],
      complemento: [''],
      bairro: ['', Validators.required],
      cidade: ['', Validators.required],
      estado: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]]
    });

    this.enderecoForm = this.formBuilder.group({
      logradouro: ['', Validators.required],
      numero: ['', Validators.required],
      complemento: [''],
      bairro: ['', Validators.required],
      cidade: ['', Validators.required],
      estado: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      cep: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(9)]]
    });
    dataRefresh.refreshRequested$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.carregarOficinas();
      if (!this.isAssistenteFrota) this.carregarEnderecos();
    });
  }

  ngOnInit(): void {
    this.carregarOficinas();
    if (!this.isAssistenteFrota) this.carregarEnderecos();
  }

  trocarAba(aba: 'oficinas' | 'enderecos'): void {
    this.abaAtiva = aba;
  }

  // ==================== OFICINA ====================

  carregarOficinas(): void {
    this.carregandoOficinas = true;
    const params: Record<string, string> = {
      page: String(this.paginaAtualOficina - 1),
      size: String(this.itensPorPagina)
    };
    if (this.termoBuscaOficina.trim()) params['busca'] = this.termoBuscaOficina.trim();
    this.http.get<PagedResponse<Oficina>>('http://localhost:8080/api/oficinas', { params }).subscribe({
      next: resposta => {
        this.totalPaginasOficina = Math.max(1, resposta.totalPages);
        this.totalOficinas = resposta.totalElements;
        if (this.paginaAtualOficina > this.totalPaginasOficina) {
          this.paginaAtualOficina = this.totalPaginasOficina;
          this.carregarOficinas();
          return;
        }
        this.oficinas = resposta.content;
        this.carregandoOficinas = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregandoOficinas = false;
        this.changeDetector.markForCheck();
        Swal.fire({ icon: 'error', title: 'Erro ao carregar', text: 'Não foi possível carregar as oficinas.' });
      }
    });
  }

  buscarOficina(termo: string): void {
    this.termoBuscaOficina = termo;
    this.paginaAtualOficina = 1;
    clearTimeout(this.buscaAgendadaOficina);
    this.buscaAgendadaOficina = setTimeout(() => this.carregarOficinas(), 300);
  }

  limparBuscaOficina(): void {
    if (this.termoBuscaOficina) this.buscarOficina('');
  }

  abrirModalOficina(): void {
    this.oficinaForm.reset();
    this.idOficinaEmEdicao = undefined;
    this.modalOficinaAberto = true;
  }

  editarOficina(oficina: Oficina): void {
    this.idOficinaEmEdicao = oficina.id;
    this.oficinaForm.patchValue({
      nomeOficina: oficina.nomeOficina,
      telefone: oficina.telefone,
      especialidade: oficina.especialidade,
      cep: oficina.endereco?.cep || '',
      logradouro: oficina.endereco?.logradouro || '',
      numero: oficina.endereco?.numero || '',
      complemento: oficina.endereco?.complemento || '',
      bairro: oficina.endereco?.bairro || '',
      cidade: oficina.endereco?.cidade || '',
      estado: oficina.endereco?.estado || ''
    });
    this.oficinaForm.markAsPristine();
    this.modalOficinaAberto = true;
  }

  buscarCepOficina(): void {
    const cep = String(this.oficinaForm.controls['cep'].value || '').replace(/\D/g, '');
    if (cep.length !== 8) return;
    this.buscandoCepOficina = true;
    this.http.get<ViaCepResposta>(`http://localhost:8080/api/enderecos/consulta-cep/${cep}`).pipe(
      timeout(8000),
      finalize(() => {
        this.buscandoCepOficina = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resposta => {
        if (resposta.erro) {
          Swal.fire({ icon: 'warning', title: 'CEP não encontrado', text: 'Confira o CEP informado.' });
          return;
        }
        this.oficinaForm.patchValue({
          cep: resposta.cep,
          logradouro: resposta.logradouro,
          bairro: resposta.bairro,
          cidade: resposta.localidade,
          estado: resposta.uf
        });
      },
      error: () => Swal.fire({ icon: 'error', title: 'Erro ao consultar CEP', text: 'Não foi possível consultar o ViaCEP.' })
    });
  }

  fecharModalOficina(): void {
    if (this.salvandoOficina) return;
    if (!this.oficinaForm.dirty) {
      this.modalOficinaAberto = false;
      return;
    }
    Swal.fire({
      icon: 'warning',
      title: 'Descartar oficina?',
      text: 'Os dados preenchidos serão perdidos.',
      showCancelButton: true,
      confirmButtonText: 'Descartar',
      cancelButtonText: 'Continuar',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (result.isConfirmed) {
        this.oficinaForm.reset();
        this.modalOficinaAberto = false;
      }
    });
  }

  salvarOficina(): void {
    if (this.oficinaForm.invalid) {
      this.oficinaForm.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Dados incompletos', text: 'Preencha todos os campos obrigatórios da oficina.' });
      return;
    }
    const dados = this.oficinaForm.getRawValue();
    const payload = {
      nomeOficina: dados.nomeOficina,
      telefone: dados.telefone,
      especialidade: dados.especialidade,
      endereco: {
        logradouro: dados.logradouro,
        numero: dados.numero,
        complemento: dados.complemento || '',
        bairro: dados.bairro,
        cidade: dados.cidade,
        estado: dados.estado,
        cep: dados.cep
      }
    };

    this.salvandoOficina = true;
    const requisicao = this.idOficinaEmEdicao
      ? this.http.put<Oficina>(`http://localhost:8080/api/oficinas/${this.idOficinaEmEdicao}`, payload)
      : this.http.post<Oficina>('http://localhost:8080/api/oficinas', payload);

    requisicao.pipe(timeout(10000), finalize(() => {
      this.salvandoOficina = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: oficina => {
        if (!this.idOficinaEmEdicao) {
          this.paginaAtualOficina = Math.max(1, Math.ceil((this.totalOficinas + 1) / this.itensPorPagina));
        }
        this.modalOficinaAberto = false;
        this.oficinaForm.reset();
        this.idOficinaEmEdicao = undefined;
        this.carregarOficinas();
        Swal.fire({ icon: 'success', title: 'Oficina salva com sucesso!', timer: 1800, showConfirmButton: false });
      },
      error: (err) => {
        const msg = err?.error?.message || 'Não foi possível salvar a oficina.';
        Swal.fire({ icon: 'error', title: 'Erro ao salvar', text: msg });
      }
    });
  }

  excluirOficina(oficina: Oficina): void {
    Swal.fire({
      icon: 'warning',
      title: 'Excluir oficina?',
      text: `${oficina.nomeOficina} será removida.`,
      showCancelButton: true,
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (!result.isConfirmed || !oficina.id) return;
      this.http.delete(`http://localhost:8080/api/oficinas/${oficina.id}`).pipe(timeout(10000)).subscribe({
        next: () => {
          this.carregarOficinas();
          Swal.fire({ icon: 'success', title: 'Oficina excluída', timer: 1600, showConfirmButton: false });
        },
        error: () => Swal.fire({ icon: 'error', title: 'Erro ao excluir', text: 'Não foi possível excluir a oficina.' })
      });
    });
  }

  mudarPaginaOficina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginasOficina) {
      this.paginaAtualOficina = pagina;
      this.carregarOficinas();
    }
  }

  // ==================== ENDEREÇO ====================

  carregarEnderecos(): void {
    this.carregandoEnderecos = true;
    const params: Record<string, string> = {
      page: String(this.paginaAtualEndereco - 1),
      size: String(this.itensPorPagina)
    };
    if (this.termoBuscaEndereco.trim()) params['busca'] = this.termoBuscaEndereco.trim();
    this.http.get<PagedResponse<Endereco>>('http://localhost:8080/api/enderecos', { params }).subscribe({
      next: resposta => {
        this.totalPaginasEndereco = Math.max(1, resposta.totalPages);
        this.totalEnderecos = resposta.totalElements;
        if (this.paginaAtualEndereco > this.totalPaginasEndereco) {
          this.paginaAtualEndereco = this.totalPaginasEndereco;
          this.carregarEnderecos();
          return;
        }
        this.enderecos = resposta.content;
        this.carregandoEnderecos = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.carregandoEnderecos = false;
        this.changeDetector.markForCheck();
        Swal.fire({ icon: 'error', title: 'Erro ao carregar', text: 'Não foi possível carregar os endereços.' });
      }
    });
  }

  buscarEndereco(termo: string): void {
    this.termoBuscaEndereco = termo;
    this.paginaAtualEndereco = 1;
    clearTimeout(this.buscaAgendadaEndereco);
    this.buscaAgendadaEndereco = setTimeout(() => this.carregarEnderecos(), 300);
  }

  limparBuscaEndereco(): void {
    if (this.termoBuscaEndereco) this.buscarEndereco('');
  }

  abrirModalEndereco(): void {
    this.enderecoForm.reset();
    this.idEnderecoEmEdicao = undefined;
    this.modalEnderecoAberto = true;
  }

  editarEndereco(endereco: Endereco): void {
    this.idEnderecoEmEdicao = endereco.id;
    this.enderecoForm.patchValue(endereco);
    this.enderecoForm.markAsPristine();
    this.modalEnderecoAberto = true;
  }

  buscarCepEndereco(): void {
    const cep = String(this.enderecoForm.controls['cep'].value || '').replace(/\D/g, '');
    if (cep.length !== 8) return;
    this.buscandoCepEndereco = true;
    this.http.get<ViaCepResposta>(`http://localhost:8080/api/enderecos/consulta-cep/${cep}`).pipe(
      timeout(8000),
      finalize(() => {
        this.buscandoCepEndereco = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: resposta => {
        if (resposta.erro) {
          Swal.fire({ icon: 'warning', title: 'CEP não encontrado', text: 'Confira o CEP informado.' });
          return;
        }
        this.enderecoForm.patchValue({
          cep: resposta.cep,
          logradouro: resposta.logradouro,
          bairro: resposta.bairro,
          cidade: resposta.localidade,
          estado: resposta.uf
        });
      },
      error: () => Swal.fire({ icon: 'error', title: 'Erro ao consultar CEP', text: 'Não foi possível consultar o ViaCEP.' })
    });
  }

  fecharModalEndereco(): void {
    if (this.salvandoEndereco) return;
    if (!this.enderecoForm.dirty) {
      this.modalEnderecoAberto = false;
      return;
    }
    Swal.fire({
      icon: 'warning',
      title: 'Descartar endereço?',
      text: 'Os dados preenchidos serão perdidos.',
      showCancelButton: true,
      confirmButtonText: 'Descartar',
      cancelButtonText: 'Continuar',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (result.isConfirmed) {
        this.enderecoForm.reset();
        this.modalEnderecoAberto = false;
      }
    });
  }

  salvarEndereco(): void {
    if (this.enderecoForm.invalid) {
      this.enderecoForm.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Dados incompletos', text: 'Preencha os campos obrigatórios do endereço.' });
      return;
    }
    this.salvandoEndereco = true;
    const requisicao = this.idEnderecoEmEdicao
      ? this.http.put<Endereco>(`http://localhost:8080/api/enderecos/${this.idEnderecoEmEdicao}`, this.enderecoForm.getRawValue())
      : this.http.post<Endereco>('http://localhost:8080/api/enderecos', this.enderecoForm.getRawValue());

    requisicao.pipe(timeout(10000), finalize(() => {
      this.salvandoEndereco = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: endereco => {
        if (!this.idEnderecoEmEdicao) {
          this.paginaAtualEndereco = Math.max(1, Math.ceil((this.totalEnderecos + 1) / this.itensPorPagina));
        }
        this.modalEnderecoAberto = false;
        this.enderecoForm.reset();
        this.idEnderecoEmEdicao = undefined;
        this.carregarEnderecos();
        Swal.fire({ icon: 'success', title: 'Endereço salvo', timer: 1800, showConfirmButton: false });
      },
      error: () => Swal.fire({ icon: 'error', title: 'Erro ao salvar', text: 'Não foi possível salvar o endereço.' })
    });
  }

  excluirEndereco(endereco: Endereco): void {
    Swal.fire({
      icon: 'warning',
      title: 'Excluir endereço?',
      text: `${endereco.logradouro}, ${endereco.numero} será removido.`,
      showCancelButton: true,
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c'
    }).then(result => {
      if (!result.isConfirmed || !endereco.id) return;
      this.http.delete(`http://localhost:8080/api/enderecos/${endereco.id}`).pipe(timeout(10000)).subscribe({
        next: () => {
          this.carregarEnderecos();
          Swal.fire({ icon: 'success', title: 'Endereço excluído', timer: 1600, showConfirmButton: false });
        },
        error: () => Swal.fire({ icon: 'error', title: 'Erro ao excluir', text: 'Não foi possível excluir o endereço.' })
      });
    });
  }

  mudarPaginaEndereco(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginasEndereco) {
      this.paginaAtualEndereco = pagina;
      this.carregarEnderecos();
    }
  }
}
