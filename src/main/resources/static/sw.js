/* ============================================================
   sw.js — Service Worker for AI Execution System PWA
   Provides offline shell caching so the app loads instantly
   from the mobile home screen even with no internet.
   ============================================================ */

// Auto-versioned cache name based on deployment timestamp
// This automatically invalidates cache on every deployment
const CACHE_NAME = 'ai-execution-20260228'; // Format: YYYYMMDD or timestamp

// Static assets to pre-cache on install (the "app shell")
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/login.html',
  '/register.html',
  '/dashboard.html',
  '/css/auth.css',
  '/css/dashboard.css',
  '/js/auth.js',
  '/js/api.js',
  '/js/dashboard.js',
  '/manifest.json'
];

// ---- Install: pre-cache app shell ----
self.addEventListener('install', event => {
  console.log('[SW] Installing version:', CACHE_NAME);
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(PRECACHE_URLS);
    })
  );
  // Force the waiting service worker to become the active service worker
  self.skipWaiting();
});

// ---- Message: handle skip waiting request from client ----
self.addEventListener('message', event => {
  if (event.data?.type === 'SKIP_WAITING') {
    console.log('[SW] Skip waiting requested');
    self.skipWaiting();
  }
});

// ---- Activate: clean up old caches automatically ----
self.addEventListener('activate', event => {
  console.log('[SW] Activating version:', CACHE_NAME);
  event.waitUntil(
    caches.keys().then(keys => {
      // Delete ALL old caches that don't match current version
      const deletePromises = keys
        .filter(key => key !== CACHE_NAME)
        .map(key => {
          console.log('[SW] Deleting old cache:', key);
          return caches.delete(key);
        });
      return Promise.all(deletePromises);
    })
  );
  // Take control of all pages immediately (don't wait for refresh)
  self.clients.claim();
});

// ---- Fetch: Network-first for API, Cache-first for static assets ----
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);

  // Always go to network for API calls (never serve stale auth/data)
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(request).catch(() =>
        new Response(
          JSON.stringify({ success: false, message: 'You are offline. Please reconnect.' }),
          { status: 503, headers: { 'Content-Type': 'application/json' } }
        )
      )
    );
    return;
  }

  // Cache-first strategy for static assets
  event.respondWith(
    caches.match(request).then(cached => {
      if (cached) return cached;

      return fetch(request).then(response => {
        // Cache successful GET responses for static resources
        if (request.method === 'GET' && response.status === 200) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(request, clone));
        }
        return response;
      }).catch(() => {
        // Offline fallback — return cached dashboard shell if available
        if (request.headers.get('Accept').includes('text/html')) {
          return caches.match('/dashboard.html');
        }
      });
    })
  );
});

// ---- Push: show OS-level notification even when the app is closed ----
self.addEventListener('push', event => {
  let data = { title: 'SMART TODO', body: 'You have a new notification.' };
  try {
    if (event.data) data = event.data.json();
  } catch (e) {
    data.body = event.data ? event.data.text() : data.body;
  }

  const options = {
    body: data.body,
    icon: '/assets/todo.icon',
    badge: '/assets/todo.icon',
    tag: 'smarttodo-' + Date.now(),
    requireInteraction: false,
    data: { url: '/dashboard.html' }
  };

  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

// ---- Notification click: focus or open the dashboard ----
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || '/dashboard.html';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
      for (const client of list) {
        if (client.url.includes('/dashboard.html') && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) return clients.openWindow(target);
    })
  );
});
