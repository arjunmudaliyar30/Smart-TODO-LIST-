/* ============================================================
   api.js — authenticated API client (used on dashboard)
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

// ---- JWT helpers (duplicated here so api.js is self-contained) ----

function decodeJwtPayload(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch (_) { return null; }
}

function isTokenValid() {
  const token = localStorage.getItem('token');
  if (!token) return false;
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) return false;
  return payload.exp * 1000 > Date.now();
}

function getToken() {
  return localStorage.getItem('token');
}

function authHeaders(extra = {}) {
  return {
    'Authorization': `Bearer ${getToken()}`,
    'Content-Type': 'application/json',
    ...extra
  };
}

function handleUnauthenticated() {
  localStorage.clear();
  window.location.href = 'login.html';
}

async function apiRequest(method, path, body = null) {
  // Client-side token expiry guard — avoids 401 round-trips
  if (!isTokenValid()) {
    handleUnauthenticated();
    return;
  }

  const opts = { method, headers: authHeaders() };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(BASE_URL + path, opts);

  if (res.status === 401) {
    handleUnauthenticated();
    return;
  }
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Request failed');
  return json;
}

async function apiGet(path)          { return apiRequest('GET', path); }
async function apiPost(path, body)   { return apiRequest('POST', path, body); }
async function apiPut(path, body)    { return apiRequest('PUT', path, body); }
async function apiPatch(path, body)  { return apiRequest('PATCH', path, body); }
async function apiDelete(path)       { return apiRequest('DELETE', path); }

async function apiUpload(path, formData) {
  if (!isTokenValid()) { handleUnauthenticated(); return; }

  const res = await fetch(BASE_URL + path, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${getToken()}` },
    body: formData
  });
  if (res.status === 401) { handleUnauthenticated(); return; }
  const json = await res.json();
  if (!res.ok) throw new Error(json.message || 'Upload failed');
  return json;
}
