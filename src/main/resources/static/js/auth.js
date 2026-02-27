/* ============================================================
   auth.js — shared auth utilities for login / register pages
   ============================================================ */

// Auto-detect backend: if served from Live Server (non-8080 port), point to Spring Boot
const BASE_URL = (window.location.port && window.location.port !== '8080')
  ? 'http://localhost:8080'
  : '';

// ---- Service Worker (PWA) registration ----
// Only register SW when served from Spring Boot (port 8080) — Live Server doesn't serve sw.js
if ('serviceWorker' in navigator && window.location.port !== '5500') {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js')
      .then(reg => console.log('[SW] Registered, scope:', reg.scope))
      .catch(err => console.warn('[SW] Registration failed:', err));
  });
}

// ---- JWT helpers ----

/**
 * Decode a JWT payload without verifying the signature.
 * Verification is always done server-side.
 */
function decodeJwtPayload(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch (_) {
    return null;
  }
}

/**
 * Returns true if the stored JWT is present AND not yet expired (client-side check).
 * The server always does the authoritative check; this just prevents pointless requests.
 */
function isTokenValid() {
  const token = localStorage.getItem('token');
  if (!token) return false;
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) return false;
  // exp is in seconds; Date.now() is in milliseconds
  return payload.exp * 1000 > Date.now();
}

async function apiPost(url, body) {
  const res = await fetch(BASE_URL + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Request failed');
  return json;
}

// ---- Redirect logged-in users away from auth pages ----
(function () {
  if (isTokenValid()) {
    const page = window.location.pathname;
    if (page.includes('login.html') || page.includes('register.html') || page === '/' || page.endsWith('index.html')) {
      window.location.href = 'dashboard.html';
    }
  }
})();
