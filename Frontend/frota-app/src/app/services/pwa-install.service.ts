import { isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import Swal from 'sweetalert2';

interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed';
    platform: string;
  }>;
  prompt(): Promise<void>;
}

@Injectable({
  providedIn: 'root'
})
export class PwaInstallService {
  private deferredPrompt: BeforeInstallPromptEvent | null = null;
  private readonly isBrowser: boolean;

  readonly canInstall = signal<boolean>(false);
  readonly isInstalled = signal<boolean>(false);
  readonly isIos = signal<boolean>(false);
  readonly showBanner = signal<boolean>(false);

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);

    if (this.isBrowser) {
      this.verificarEstadoInstalacao();
      this.configurarOuvintesPwa();
      this.registrarSwDevSeNecessario();
    }
  }

  private verificarEstadoInstalacao(): void {
    // Detecta se já está rodando em modo standalone (como app instalado)
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      window.matchMedia('(display-mode: window-controls-overlay)').matches ||
      (window.navigator as unknown as { standalone?: boolean }).standalone === true ||
      document.referrer.includes('android-app://');

    this.isInstalled.set(Boolean(isStandalone));

    // Detecta iOS
    const ua = window.navigator.userAgent.toLowerCase();
    const isIosDevice = /iphone|ipad|ipod/.test(ua);
    this.isIos.set(isIosDevice);

    // Se já estiver instalado, não exibe banner
    if (isStandalone) {
      this.showBanner.set(false);
      return;
    }

    // Verifica se usuário já dispensou o banner recentemente
    const dispensado = localStorage.getItem('frota-pwa-banner-dispensado');
    const agora = Date.now();
    if (!dispensado || agora - Number(dispensado) > 3 * 24 * 60 * 60 * 1000) {
      // Exibe banner após 2 segundos para não sobrecarregar carregamento inicial
      setTimeout(() => {
        if (!this.isInstalled()) {
          this.showBanner.set(true);
        }
      }, 2000);
    }
  }

  private configurarOuvintesPwa(): void {
    // Captura o evento nativo de instalação do Chrome / Edge / Android
    window.addEventListener('beforeinstallprompt', (event: Event) => {
      event.preventDefault();
      this.deferredPrompt = event as BeforeInstallPromptEvent;
      this.canInstall.set(true);
      if (!this.isInstalled()) {
        this.showBanner.set(true);
      }
    });

    // Detecta conclusão da instalação
    window.addEventListener('appinstalled', () => {
      this.deferredPrompt = null;
      this.canInstall.set(false);
      this.isInstalled.set(true);
      this.showBanner.set(false);

      Swal.fire({
        icon: 'success',
        title: 'Aplicativo instalado!',
        text: 'O Frota ASG foi instalado com sucesso. Você já pode acessá-lo diretamente pelo atalho na sua área de trabalho ou tela inicial.',
        confirmButtonColor: '#0284c7'
      });
    });
  }

  private registrarSwDevSeNecessario(): void {
    // Em localhost ou desenvolvimento, registra o pwa-sw.js se não houver SW ativo
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.getRegistration().then((reg) => {
        if (!reg) {
          navigator.serviceWorker.register('/pwa-sw.js').catch(() => {
            // Silencioso em caso de restrição local
          });
        }
      });
    }
  }

  /**
   * Dispara a instalação do aplicativo ou orienta o usuário
   */
  async promptInstall(): Promise<void> {
    if (this.deferredPrompt) {
      try {
        await this.deferredPrompt.prompt();
        const choice = await this.deferredPrompt.userChoice;
        if (choice.outcome === 'accepted') {
          this.isInstalled.set(true);
          this.canInstall.set(false);
          this.showBanner.set(false);
        }
        this.deferredPrompt = null;
      } catch (err) {
        console.warn('Erro ao abrir prompt de instalação:', err);
      }
      return;
    }

    if (this.isIos()) {
      Swal.fire({
        title: 'Instalar no iPhone / iPad',
        html: `
          <div style="text-align: left; font-size: 15px; line-height: 1.6; color: #334155;">
            <p>Para adicionar o <strong>Frota ASG</strong> à tela de início do seu dispositivo:</p>
            <ol style="margin-top: 10px; padding-left: 20px;">
              <li>Toque no botão de <strong>Compartilhar</strong> (ícone com quadrado e seta para cima no Safari)</li>
              <li>Role as opções e toque em <strong>"Adicionar à Tela de Início"</strong></li>
              <li>Toque em <strong>"Adicionar"</strong> no canto superior direito</li>
            </ol>
            <p style="margin-top: 12px; font-weight: 500; color: #0284c7;">
              Pronto! Um atalho exclusivo do aplicativo estará disponível na tela do seu aparelho.
            </p>
          </div>
        `,
        icon: 'info',
        confirmButtonText: 'Entendi',
        confirmButtonColor: '#0284c7'
      });
      return;
    }

    // Orientação para navegadores de computador (Chrome/Edge) ou Android
    Swal.fire({
      title: 'Instalar e criar atalho',
      html: `
        <div style="text-align: left; font-size: 15px; line-height: 1.6; color: #334155;">
          <p>Você pode instalar o <strong>Frota ASG</strong> como um aplicativo no seu computador ou celular:</p>
          <div style="background: #f1f5f9; border-radius: 8px; padding: 12px; margin: 12px 0;">
            <p style="margin: 0 0 6px 0; font-weight: 600; color: #0f172a;">🖥️ No Computador (Google Chrome ou Microsoft Edge):</p>
            <ul style="margin: 0; padding-left: 20px; font-size: 14px;">
              <li>Clique no ícone de instalar <strong>(⊕ ou monitor com seta)</strong> na barra de endereços (ao lado dos favoritos).</li>
              <li>Ou abra o menu do navegador <strong>(três pontinhos ⋮)</strong> e clique em <strong>"Instalar Frota ASG..."</strong></li>
            </ul>
          </div>
          <div style="background: #f1f5f9; border-radius: 8px; padding: 12px; margin: 12px 0;">
            <p style="margin: 0 0 6px 0; font-weight: 600; color: #0f172a;">📱 No Celular (Android / Chrome):</p>
            <ul style="margin: 0; padding-left: 20px; font-size: 14px;">
              <li>Abra o menu do navegador <strong>(três pontinhos ⋮)</strong></li>
              <li>Selecione <strong>"Instalar aplicativo"</strong> ou <strong>"Adicionar à tela inicial"</strong></li>
            </ul>
          </div>
          <p style="margin-top: 10px; font-size: 13px; color: #64748b;">
            O aplicativo funcionará em janela própria e criará um atalho oficial na sua área de trabalho e barra de tarefas.
          </p>
        </div>
      `,
      icon: 'info',
      confirmButtonText: 'Fechar',
      confirmButtonColor: '#0284c7'
    });
  }

  dispensarBanner(): void {
    this.showBanner.set(false);
    if (this.isBrowser) {
      localStorage.setItem('frota-pwa-banner-dispensado', String(Date.now()));
    }
  }
}
