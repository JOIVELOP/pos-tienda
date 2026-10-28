import { defineConfig, Plugin } from 'vite'
import react from '@vitejs/plugin-react-swc'

function offlineShell(): Plugin {
  return {
    name: 'pos-offline-shell',
    enforce: 'post',
    apply: 'build',
    generateBundle(_options, bundle) {
      const assets = Object.keys(bundle).filter(name => !name.endsWith('.map'));
      const version = `pos-shell-${Date.now()}`;
      this.emitFile({
        type: 'asset',
        fileName: 'sw.js',
        source: `
const CACHE = ${JSON.stringify(version)};
const ASSETS = ${JSON.stringify(assets)};
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(ASSETS)));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys
    .filter(key => key.startsWith('pos-shell-') && key !== CACHE)
    .map(key => caches.delete(key)))));
});
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin ||
      url.pathname.startsWith('/api/')) return;
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(() =>
      caches.open(CACHE).then(cache => cache.match('index.html')).then(response => {
        if (!response) throw new Error('Prepara la aplicacion online antes del corte');
        return response;
      })));
  } else {
    event.respondWith(caches.open(CACHE).then(cache =>
      cache.match(event.request).then(response => response || fetch(event.request))));
  }
});
`,
      });
    },
  };
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), offlineShell()],
})
