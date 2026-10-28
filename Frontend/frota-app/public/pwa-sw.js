// Service worker leve para habilitar instalação PWA em ambiente de desenvolvimento e rede local
const SW_VERSION = 'frota-asg-pwa-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Pass-through de requisições sem interferir no HMR ou hot-reload de desenvolvimento
  event.respondWith(
    fetch(event.request).catch(() => {
      // Se offline e requisição de navegação, tenta servir a página principal se estiver em cache
      if (event.request.mode === 'navigate') {
        return caches.match('/');
      }
      return Promise.reject('Offline');
    })
  );
});
