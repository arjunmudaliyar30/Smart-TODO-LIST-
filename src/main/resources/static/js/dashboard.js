/* ============================================================
   dashboard.js — Mobile-first dashboard controller
   ============================================================ */

// ---- Theme (Day / Night) ----
(function () {
  if (localStorage.getItem('theme') === 'light') {
    document.body.classList.add('light-mode');
  }
})();

function toggleTheme() {
  const isLight = document.body.classList.toggle('light-mode');
  localStorage.setItem('theme', isLight ? 'light' : 'dark');
  const btn = document.getElementById('themeToggleBtn');
  if (btn) btn.textContent = isLight ? '\uD83C\uDF19' : '\uD83C\uDF1E';
}

// ---- Settings ----
function openSettings() {
  // Load current preferences from profile
  apiGet('/api/users/me').then(function(res) {
    if (!res || !res.data) return;
    var u = res.data;
    var hr = (u.dailySummaryHour != null ? u.dailySummaryHour : 8);
    var hh = String(hr).padStart(2, '0');
    var el_time = document.getElementById('settDailySummaryTime');
    if (el_time) el_time.value = hh + ':00';
    var el_push = document.getElementById('settPushEnabled');
    if (el_push) el_push.checked = !!u.pushNotificationsEnabled;
    var el_email = document.getElementById('settEmailEnabled');
    var emailEnabled = u.preferences && u.preferences.emailNotificationsEnabled === 'true';
    if (el_email) el_email.checked = emailEnabled;
    var el_tone = document.getElementById('settAiTone');
    if (el_tone) el_tone.value = (u.preferences && u.preferences.aiTone) ? u.preferences.aiTone : 'friendly';
    // Morning alarm
    var el_alarmTime = document.getElementById('settMorningAlarm');
    if (el_alarmTime) el_alarmTime.value = (u.preferences && u.preferences.morningAlarmTime) ? u.preferences.morningAlarmTime : '07:00';
    var el_alarmEnabled = document.getElementById('settMorningAlarmEnabled');
    if (el_alarmEnabled) el_alarmEnabled.checked = !!(u.preferences && u.preferences.morningAlarmEnabled === 'true');
    // Profile display
    var nameEl = document.getElementById('settProfileName');
    var emailEl = document.getElementById('settProfileEmail');
    var initialEl = document.getElementById('settProfileInitial');
    var fn = u.fullName || localStorage.getItem('fullName') || 'User';
    if (nameEl) nameEl.textContent = fn;
    if (emailEl) emailEl.textContent = u.email || '';
    if (initialEl) {
      var parts = fn.trim().split(/\s+/);
      var initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0][0];
      initialEl.textContent = initials.toUpperCase();
    }
  }).catch(function() {
    // Fallback from localStorage if API fails
    var fn = localStorage.getItem('fullName') || 'User';
    var nameEl = document.getElementById('settProfileName');
    var initialEl = document.getElementById('settProfileInitial');
    if (nameEl) nameEl.textContent = fn;
    if (initialEl) {
      var parts = fn.trim().split(/\s+/);
      var initials = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0][0];
      initialEl.textContent = initials.toUpperCase();
    }
  });
  openModal('settingsModal');
}

async function checkForUpdates() {
  if (!('serviceWorker' in navigator)) {
    location.reload(true);
    return;
  }
  try {
    const reg = await navigator.serviceWorker.getRegistration();
    if (!reg) { location.reload(true); return; }
    await reg.update();
    if (reg.waiting) {
      reg.waiting.postMessage({ type: 'SKIP_WAITING' });
      toast('Update ready! Reloading…', 'success');
      setTimeout(() => location.reload(true), 1200);
    } else if (reg.installing) {
      toast('Downloading update…', 'success');
      reg.installing.addEventListener('statechange', function() {
        if (this.state === 'installed') {
          this.postMessage({ type: 'SKIP_WAITING' });
          setTimeout(() => location.reload(true), 800);
        }
      });
    } else {
      toast('App is already up to date ✅', 'success');
    }
  } catch (e) {
    location.reload(true);
  }
}

async function saveSettings() {
  var timeVal = document.getElementById('settDailySummaryTime')?.value || '08:00';
  var hourStr = timeVal.split(':')[0];
  var dailySummaryHour = parseInt(hourStr, 10) || 8;
  var pushEnabled  = !!document.getElementById('settPushEnabled')?.checked;
  var emailEnabled = !!document.getElementById('settEmailEnabled')?.checked;
  var aiTone = document.getElementById('settAiTone')?.value || 'friendly';
  var morningAlarmTime = document.getElementById('settMorningAlarm')?.value || '07:00';
  var morningAlarmEnabled = !!document.getElementById('settMorningAlarmEnabled')?.checked;
  try {
    await apiPatch('/api/users/me/preferences', {
      pushNotificationsEnabled: pushEnabled,
      dailySummaryEnabled: true,
      dailySummaryHour: dailySummaryHour,
      emailNotificationsEnabled: emailEnabled,
      aiTone: aiTone,
      morningAlarmTime: morningAlarmTime,
      morningAlarmEnabled: String(morningAlarmEnabled)
    });
    // Schedule tomorrow's morning alarm if enabled
    if (morningAlarmEnabled) {
      var [alarmHr, alarmMin] = morningAlarmTime.split(':').map(Number);
      var tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      tomorrow.setHours(alarmHr, alarmMin, 0, 0);
      var pad = n => String(n).padStart(2,'0');
      var d = tomorrow;
      var iso = d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())+'T'+pad(d.getHours())+':'+pad(d.getMinutes())+':00';
      apiPost('/api/alarms', { title: '⏰ Morning Alarm', scheduledAt: iso }).catch(function(){});
    }
    toast('Settings saved ✅', 'success');
    closeModal('settingsModal');
  } catch (err) {
    toast(err.message || 'Failed to save settings', 'error');
  }
}

function shareApp() {
  var shareData = {
    title: 'FORGE – Smart TODO & Fitness Tracker',
    text: 'Check out FORGE, an AI-powered productivity app!',
    url: window.location.origin
  };
  if (navigator.share) {
    navigator.share(shareData).catch(function() {});
  } else {
    navigator.clipboard?.writeText(window.location.origin).then(function() {
      toast('App link copied to clipboard!', 'success');
    }).catch(function() {
      toast('Share: ' + window.location.origin, 'info');
    });
  }
}

// Apply correct icon on load
window.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('themeToggleBtn');
  if (btn) btn.textContent =
    document.body.classList.contains('light-mode') ? '\uD83C\uDF19' : '\uD83C\uDF1E';

  // Inject credit footer as direct body child — always visible on every tab/module
  if (!document.getElementById('appCreditFooter')) {
    const footer = document.createElement('div');
    footer.id = 'appCreditFooter';
    footer.className = 'app-credit-footer';
    footer.innerHTML =
      'Developed with <span class="credit-heart">\u2764</span> by ' +
      '<a href="https://arjun-portfolio-wheat.vercel.app/" target="_blank" ' +
      'rel="noopener noreferrer" class="credit-link">Arjun Ramaswamy Mudaliyar</a>';
    document.body.appendChild(footer);
  }
});

// ---- Auth guard ----
(function () {
  if (!isTokenValid()) {
    window.location.href = 'login.html';
  }
})();

// ---- PWA Install Prompt ----
let _deferredInstallPrompt = null;
// Never show install prompt when already running as an installed PWA
const _isStandalone = window.matchMedia('(display-mode: standalone)').matches
                   || window.navigator.standalone === true;
if (!_isStandalone) {
  window.addEventListener('beforeinstallprompt', e => {
    e.preventDefault();
    _deferredInstallPrompt = e;
    const btn = document.getElementById('installBtn');
    if (btn) btn.classList.remove('hidden');
  });
  window.addEventListener('appinstalled', () => {
    const btn = document.getElementById('installBtn');
    if (btn) btn.classList.add('hidden');
    _deferredInstallPrompt = null;
    toast('App added to your home screen! ✅');
  });
}

// ---- Web Push Notifications (works even when the app is closed) ----
async function initPushNotifications() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;
  try {
    // Get VAPID public key
    const res = await fetch('/api/push/public-key');
    if (!res.ok) return;
    const { publicKey } = await res.json();
    if (!publicKey) return;

    const reg = await navigator.serviceWorker.ready;

    // Check existing subscription first
    let sub = await reg.pushManager.getSubscription();
    if (!sub) {
      // Request permission
      const perm = await Notification.requestPermission();
      if (perm !== 'granted') return;
      // Subscribe
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey)
      });
    }

    // Send subscription to backend
    const token = localStorage.getItem('token');
    if (!token) return;
    await fetch('/api/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({
        endpoint: sub.endpoint,
        p256dh:   arrayBufferToBase64Url(sub.getKey('p256dh')),
        auth:     arrayBufferToBase64Url(sub.getKey('auth'))
      })
    });
    console.log('[Push] Subscribed ✅');
  } catch (err) {
    console.warn('[Push] Subscription failed:', err.message);
  }
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64  = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw     = atob(base64);
  return Uint8Array.from([...raw].map(c => c.charCodeAt(0)));
}

function arrayBufferToBase64Url(buffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

// ---- Service Worker Registration & Auto-Update ----
async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  try {
    const registration = await navigator.serviceWorker.register('/sw.js');
    console.log('[SW] Registered:', registration.scope);
    
    // Check for updates every 5 minutes
    setInterval(() => registration.update(), 5 * 60 * 1000);
    
    // Listen for new service worker waiting to activate
    registration.addEventListener('updatefound', () => {
      const newWorker = registration.installing;
      if (!newWorker) return;
      
      newWorker.addEventListener('statechange', () => {
        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
          // New version available!
          console.log('[SW] New version detected');
          showUpdateNotification(registration);
        }
      });
    });
    
    // If there's already a waiting SW, show notification immediately
    if (registration.waiting) {
      showUpdateNotification(registration);
    }
  } catch (err) {
    console.warn('[SW] Registration failed:', err);
  }
}

function showUpdateNotification(registration) {
  // Auto-reload after 3 seconds to apply update
  toast('New version available! Reloading in 3 seconds...', 'info');
  setTimeout(() => {
    registration.waiting?.postMessage({ type: 'SKIP_WAITING' });
    window.location.reload();
  }, 3000);
}

// Reload when new service worker takes control
navigator.serviceWorker?.addEventListener('controllerchange', () => {
  window.location.reload();
});

// Kick off push subscription once page is ready
window.addEventListener('load', () => {
  registerServiceWorker();
  initPushNotifications();
});


// ---- State ----
let activeChatSessionId = null;
let allTasks = [];
let allGoals = [];
let _taskTab = 'all';

// ---- Notes state (declared here to avoid TDZ when accessed before line 2300+) ----
let _noteCurrentDate   = null;
let _noteData          = null;
let _noteAutoSaveTimer = null;

// ---- Helpers ----
function toast(msg, type = 'success') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = `toast ${type}`;
  void el.offsetWidth;
  setTimeout(() => el.classList.add('hidden'), 3500);
}

function formatDate(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function badgeStatus(status) {
  const map = {
    PENDING: 'badge-pending',
    IN_PROGRESS: 'badge-progress',
    COMPLETED: 'badge-completed',
    DONE: 'badge-done'
  };
  return `<span class="badge ${map[status] || ''}">${status || ''}</span>`;
}

function badgePriority(p) {
  const map = { HIGH: 'badge-high', URGENT: 'badge-high', MEDIUM: 'badge-medium', LOW: 'badge-low' };
  return p ? `<span class="badge ${map[p] || ''}">${p}</span>` : '';
}

function esc(str) {
  const d = document.createElement('div');
  d.textContent = str || '';
  return d.innerHTML;
}

function fileSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

// ---- Navigation (bottom nav) ----
document.querySelectorAll('.nav-item').forEach(link => {
  link.addEventListener('click', e => {
    e.preventDefault();
    const tab = link.dataset.tab;
    if (!tab) return;
    document.querySelectorAll('.nav-item').forEach(l => l.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    link.classList.add('active');
    const tabEl = document.getElementById(`tab-${tab}`);
    if (tabEl) tabEl.classList.add('active');

    // Hide fitness FAB when leaving fitness tab
    hideFitFab();

    if (tab === 'tasks')      loadTasks();
    if (tab === 'goals')      loadGoals();
    if (tab === 'workouts')   initFitnessTab();
    if (tab === 'files')      loadFiles();
    if (tab === 'notes')      initNotesTab();
    if (tab === 'collab')     loadCollabSection();
    if (tab === 'people')     { loadConversations(); refreshUnreadBadge(); }
    if (tab === 'braincoach') initBrainCoachTab();
  });
});

// ---- Logout ----
document.getElementById('logoutBtn').addEventListener('click', () => {
  localStorage.clear();
  window.location.href = 'login.html';
});

// ---- Install button ----
document.getElementById('installBtn').addEventListener('click', async () => {
  if (!_deferredInstallPrompt) return;
  _deferredInstallPrompt.prompt();
  const { outcome } = await _deferredInstallPrompt.userChoice;
  if (outcome === 'accepted') toast('Installing AI Powered TODO List… ⚡');
  _deferredInstallPrompt = null;
  document.getElementById('installBtn').classList.add('hidden');
});

// ---- User greeting ----
document.getElementById('userGreeting').textContent =
  'Hi, ' + (localStorage.getItem('fullName') || 'User');

// ---- Modal helpers ----
document.querySelectorAll('.modal-close, [data-modal]').forEach(btn => {
  btn.addEventListener('click', () => {
    const modalId = btn.dataset.modal;
    if (modalId) document.getElementById(modalId).classList.add('hidden');
  });
});
function openModal(id)  { document.getElementById(id).classList.remove('hidden'); }
function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

// ---- Empty State Helper ----
function emptyStateHTML(icon, title, message, btnLabel, btnOnclick) {
  const btn = btnLabel
    ? `<button class="btn-primary" style="margin-top:1rem" onclick="${btnOnclick}">${btnLabel}</button>`
    : '';
  return `<div style="text-align:center;padding:2.5rem 1rem;color:var(--text-muted)">
    <div style="font-size:3rem;margin-bottom:0.5rem">${icon}</div>
    <div style="font-size:1rem;font-weight:600;color:var(--text-primary);margin-bottom:0.25rem">${title}</div>
    <div style="font-size:0.85rem">${message}</div>
    ${btn}
  </div>`;
}

// ==========================================================================
// AI FAB + OVERLAY
// ==========================================================================
const aiFab     = document.getElementById('aiFab');
const aiOverlay = document.getElementById('aiChatOverlay');
const closeAiChat = document.getElementById('closeAiChat');

aiFab.addEventListener('click', () => {
  aiOverlay.classList.remove('hidden');
  aiFab.classList.add('open');
  loadChatHistory();
});

closeAiChat.addEventListener('click', () => {
  aiOverlay.classList.add('hidden');
  aiFab.classList.remove('open');
});

aiOverlay.addEventListener('click', e => {
  if (e.target === aiOverlay) {
    aiOverlay.classList.add('hidden');
    aiFab.classList.remove('open');
  }
});

// ---- Daily Summary ----
document.getElementById('dailySummaryBtn').addEventListener('click', async () => {
  const btn   = document.getElementById('dailySummaryBtn');
  const panel = document.getElementById('dailySummaryPanel');
  btn.disabled = true;
  btn.textContent = '⏳…';
  panel.classList.remove('hidden');
  panel.textContent = 'Generating your daily summary…';
  try {
    const res = await apiGet('/api/ai/daily-summary');
    panel.textContent = res.data || 'No summary available.';
  } catch (err) {
    panel.textContent = '⚠ ' + (err.message || 'Error');
  } finally {
    btn.disabled = false;
    btn.textContent = '📊 Summary';
  }
});

// ---- Clear Chat ----
document.getElementById('clearChatBtn').addEventListener('click', async () => {
  if (!activeChatSessionId) return;
  if (!confirm('Clear this chat session?')) return;
  try {
    await apiDelete(`/api/ai/chat/session/${activeChatSessionId}`);
    activeChatSessionId = null;
    document.getElementById('chatWindow').innerHTML = '';
    toast('Chat cleared');
  } catch (err) { toast(err.message, 'error'); }
});

// ---- Chat ----
document.getElementById('sendChatBtn').addEventListener('click', sendChat);
document.getElementById('chatInput').addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendChat(); }
});

async function loadChatHistory() {
  try {
    const res = await apiGet('/api/ai/chat/history');
    const messages = res.data || [];
    if (messages.length > 0) {
      activeChatSessionId = messages[0].sessionId;
      renderMessages(messages);
    }
  } catch (_) {}
}

function renderMessages(messages) {
  const win = document.getElementById('chatWindow');
  win.innerHTML = messages.map(m => `
    <div class="chat-bubble ${m.role === 'USER' ? 'user' : 'assistant'}">
      ${esc(m.content)}
    </div>`).join('');
  win.scrollTop = win.scrollHeight;
}

async function sendChat() {
  const input = document.getElementById('chatInput');
  const message = input.value.trim();
  if (!message) return;

  const win = document.getElementById('chatWindow');

  const userBubble = document.createElement('div');
  userBubble.className = 'chat-bubble user';
  userBubble.textContent = message;
  win.appendChild(userBubble);

  const typingBubble = document.createElement('div');
  typingBubble.className = 'chat-bubble assistant typing';
  typingBubble.textContent = 'AI is thinking…';
  win.appendChild(typingBubble);
  win.scrollTop = win.scrollHeight;

  input.value = '';
  document.getElementById('sendChatBtn').disabled = true;

  try {
    const res = await apiPost('/api/ai/chat', { message, sessionId: activeChatSessionId });
    const aiData = res.data;
    const aiMsg  = aiData ? aiData.message : null;
    if (!activeChatSessionId && aiMsg) activeChatSessionId = aiMsg.sessionId;

    typingBubble.remove();
    const aiBubble = document.createElement('div');
    aiBubble.className = 'chat-bubble assistant';
    aiBubble.textContent = aiMsg ? aiMsg.content : '…';
    win.appendChild(aiBubble);
    win.scrollTop = win.scrollHeight;

    if (aiData && aiData.taskCreated) {
      toast(`📌 Task scheduled: "${aiData.taskCreated.title}"`, 'success');
      loadTasks();
    }
    if (aiData && aiData.tasksCreated && aiData.tasksCreated.length) {
      const names = aiData.tasksCreated.map(t => `"${t.title}"`).join(', ');
      toast(`📌 ${aiData.tasksCreated.length} tasks created: ${names}`, 'success');
      // Also show a summary bubble in chat
      const summaryBubble = document.createElement('div');
      summaryBubble.className = 'chat-bubble assistant';
      summaryBubble.innerHTML = `✅ <strong>${aiData.tasksCreated.length} tasks scheduled:</strong><br>` +
        aiData.tasksCreated.map(t => `• ${t.title}${t.dueDate ? ' <span style="color:#a0a0c0;font-size:0.8em">(' + new Date(t.dueDate).toLocaleString(undefined,{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}) + ')</span>' : ''}`).join('<br>');
      win.appendChild(summaryBubble);
      win.scrollTop = win.scrollHeight;
      loadTasks();
    }
    if (aiData && aiData.goalCreated) {
      toast(`🎯 Goal added: "${aiData.goalCreated.title}"`, 'success');
      loadGoals();
    }
    if (aiData && aiData.workoutCreated) {
      toast(`💪 Workout logged: "${aiData.workoutCreated.name}"`, 'success');
      loadWorkouts();
    }
  } catch (err) {
    typingBubble.className = 'chat-bubble assistant';
    typingBubble.textContent = '⚠ ' + (err.message || 'Error');
  } finally {
    document.getElementById('sendChatBtn').disabled = false;
  }
}

// ==========================================================================
// TASKS
// ==========================================================================

document.getElementById('openTaskModal').addEventListener('click', async () => {
  // --- Populate goal dropdown ---
  const sel = document.getElementById('taskGoalId');
  if (allGoals.length === 0) {
    try { const r = await apiGet('/api/goals'); allGoals = r.data || []; } catch (_) {}
  }
  sel.innerHTML = '<option value="">— No goal —</option>' +
    allGoals.map(g => `<option value="${g.id}">${esc(g.title)}</option>`).join('');

  // --- Populate fitness category dropdown ---
  try {
    const cr = await apiGet('/api/fitness/categories');
    const cats = cr.data || [];
    const catSel = document.getElementById('taskFitnessCategoryId');
    catSel.innerHTML = '<option value="">— None —</option>' +
      cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  } catch (_) {}

  // --- Populate parent task dropdown ---
  try {
    const tr = await apiGet('/api/tasks');
    const tasks = tr.data || [];
    const parentSel = document.getElementById('taskParentId');
    parentSel.innerHTML = '<option value="">— Top-level task —</option>' +
      tasks.map(t => `<option value="${t.id}">${esc(t.title)}</option>`).join('');
  } catch (_) {}

  // --- Reset all fields ---
  document.getElementById('taskScheduledDate').value = '';
  document.getElementById('taskFitnessCategoryId').value = '';
  document.getElementById('taskCaloriesConsumed').value = '';
  document.getElementById('taskCaloriesBurned').value = '';
  document.getElementById('caloriesRow').style.display = 'none';
  document.getElementById('taskParentId').value = '';
  document.getElementById('taskDuration').value = '';
  document.getElementById('taskAutoComplete').checked = false;
  // Reset edit state
  document.getElementById('taskEditId').value = '';
  document.getElementById('taskModalTitle').textContent = 'New Task';
  document.getElementById('taskSubmitBtn').textContent = 'Create Task';
  openModal('taskModal');
});

// Add custom fitness category handler
document.getElementById('addCategoryBtn').addEventListener('click', async () => {
  const name = prompt('Enter custom category name (e.g. YOGA, SWIMMING):');
  if (!name || !name.trim()) return;
  try {
    await apiPost('/api/fitness/categories', { name: name.trim().toUpperCase() });
    toast('Category added!');
    // Refresh category dropdown
    const cr = await apiGet('/api/fitness/categories');
    const cats = cr.data || [];
    const catSel = document.getElementById('taskFitnessCategoryId');
    catSel.innerHTML = '<option value="">— None —</option>' +
      cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  } catch (err) { toast(err.message, 'error'); }
});

function _todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function setTaskTab(tab) {
  _taskTab = tab;
  document.querySelectorAll('#taskTabBar .task-tab').forEach(function(btn) {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  renderTasks(getFilteredTasks());
}

function updateTaskTabBadges() {
  var today = _todayStr();
  var todayCount  = allTasks.filter(function(t) {
    var notDone = t.status !== 'DONE' && t.status !== 'COMPLETED';
    return notDone && (t.recurring === true || (t.dueDate && t.dueDate.slice(0,10) === today));
  }).length;
  var dailyCount  = allTasks.filter(function(t) { return t.recurring === true; }).length;
  var allCount    = allTasks.length;
  var collabCount = allTasks.filter(function(t) { return t.collaboratorIds && t.collaboratorIds.length > 0; }).length;
  var el;
  el = document.getElementById('badge-today');  if (el) el.textContent = todayCount;
  el = document.getElementById('badge-daily');  if (el) el.textContent = dailyCount;
  el = document.getElementById('badge-all');    if (el) el.textContent = allCount;
  el = document.getElementById('badge-collab'); if (el) el.textContent = collabCount;
}

function getFilteredTasks() {
  const query = (document.getElementById('taskSearchInput').value || '').trim().toLowerCase();
  var today = _todayStr();
  let tasks = allTasks;
  if (_taskTab === 'today') {
    tasks = tasks.filter(function(t) {
      var notDone = t.status !== 'DONE' && t.status !== 'COMPLETED';
      var dueToday = t.dueDate && t.dueDate.slice(0,10) === today;
      return notDone && (t.recurring === true || dueToday);
    });
  } else if (_taskTab === 'daily') {
    tasks = tasks.filter(function(t) { return t.recurring === true; });
  } else if (_taskTab === 'collab') {
    tasks = tasks.filter(function(t) { return t.collaboratorIds && t.collaboratorIds.length > 0; });
  }
  // 'all' shows everything
  if (query) {
    tasks = tasks.filter(t =>
      (t.title || '').toLowerCase().includes(query) ||
      (t.description || '').toLowerCase().includes(query) ||
      (t.section || '').toLowerCase().includes(query)
    );
  }
  return tasks;
}

document.getElementById('taskStatusFilter').addEventListener('change', () => renderTasks(getFilteredTasks()));
document.getElementById('taskSectionFilter').addEventListener('change', () => renderTasks(getFilteredTasks()));

// Search input — live filter
(function() {
  const searchInput = document.getElementById('taskSearchInput');
  const clearBtn    = document.getElementById('clearTaskSearch');
  let _searchTimer  = null;
  searchInput.addEventListener('input', () => {
    clearTimeout(_searchTimer);
    clearBtn.classList.toggle('hidden', !searchInput.value);
    _searchTimer = setTimeout(() => renderTasks(getFilteredTasks()), 200);
  });
  clearBtn.addEventListener('click', () => {
    searchInput.value = '';
    clearBtn.classList.add('hidden');
    searchInput.focus();
    renderTasks(getFilteredTasks());
  });
})();

// Show/hide calories fields based on fitness category selection
document.getElementById('taskFitnessCategoryId').addEventListener('change', function() {
  const caloriesRow = document.getElementById('caloriesRow');
  caloriesRow.style.display = this.value ? 'flex' : 'none';
  if (!this.value) {
    document.getElementById('taskCaloriesConsumed').value = '';
    document.getElementById('taskCaloriesBurned').value = '';
  }
});

document.getElementById('taskForm').addEventListener('submit', async e => {
  e.preventDefault();
  const editId           = document.getElementById('taskEditId').value;
  const due              = document.getElementById('taskDue').value;
  const scheduledDate    = document.getElementById('taskScheduledDate').value || null;
  const fitnessCatId     = document.getElementById('taskFitnessCategoryId').value || null;
  const calConsumed      = document.getElementById('taskCaloriesConsumed').value;
  const calBurned        = document.getElementById('taskCaloriesBurned').value;
  const parentTaskId     = document.getElementById('taskParentId').value || null;
  const durationRaw      = document.getElementById('taskDuration').value;
  const autoComplete     = document.getElementById('taskAutoComplete').checked || null;
  const payload = {
    title:                document.getElementById('taskTitle').value,
    description:          document.getElementById('taskDesc').value,
    priority:             document.getElementById('taskPriority').value,
    dueDate:              due ? due + ':00' : null,
    category:             document.getElementById('taskCategory').value || null,
    section:              document.getElementById('taskSection').value || null,
    goalId:               document.getElementById('taskGoalId').value || null,
    scheduledDate,
    fitnessCategoryId:    fitnessCatId,
    caloriesConsumed:     calConsumed !== '' ? parseInt(calConsumed, 10) : null,
    caloriesBurned:       calBurned   !== '' ? parseInt(calBurned,   10) : null,
    parentTaskId,
    durationMinutes:      durationRaw ? parseInt(durationRaw, 10) : null,
    autoComplete,
    recurring:            document.getElementById('taskRecurring').checked,
    recurringTime:        document.getElementById('taskRecurringTime').value || null,
    alarmTime:            (() => { const v = document.getElementById('taskAlarmTime').value; return v ? v + ':00' : null; })()
  };
  try {
    if (editId) {
      await apiPut(`/api/tasks/${editId}`, payload);
      toast('Task updated!');
    } else {
      await apiPost('/api/tasks', payload);
      toast('Task created!');
      aiSuggest(`New task created: "${payload.title}". Give a quick productivity tip.`);
    }
    document.getElementById('taskModal').classList.add('hidden');
    document.getElementById('taskForm').reset();
    document.getElementById('taskEditId').value = '';
    document.getElementById('taskModalTitle').textContent = 'New Task';
    document.getElementById('taskSubmitBtn').textContent = 'Create Task';
    loadTasks();
    loadRecurringTasks();
  } catch (err) { toast(err.message, 'error'); }
});

function populateSectionFilter() {
  const sel = document.getElementById('taskSectionFilter');
  const current = sel.value;
  const sections = [...new Set(allTasks.map(t =>
    (t.section && t.section.trim()) ? t.section.trim() : 'General'
  ))].sort((a, b) => a === 'General' ? 1 : b === 'General' ? -1 : a.localeCompare(b));
  sel.innerHTML = '<option value="">All Sections</option>' +
    '<option value="__DAILY__">📅 Daily Tasks</option>' +
    sections.map(s => `<option value="${esc(s)}"${s === current ? ' selected' : ''}>${esc(s)}</option>`).join('');
  // Restore previous selection
  if (current) sel.value = current;
}

async function loadTasks() {
  document.getElementById('taskList').innerHTML = '<div class="loading">Loading tasks\u2026</div>';
  try {
    const res = await apiGet('/api/tasks');
    allTasks = res.data || [];
    populateSectionFilter();
    updateTaskTabBadges();
    renderTasks(getFilteredTasks());
  } catch (err) { toast(err.message, 'error'); }
}

const PRIORITY_ORDER = { URGENT: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

// Track whether the "Done" section is collapsed
let _doneCollapsed = false;
function toggleDoneSection() {
  _doneCollapsed = !_doneCollapsed;
  const body = document.getElementById('doneSectionBody');
  const chevron = document.getElementById('doneSectionChevron');
  if (body) body.style.display = _doneCollapsed ? 'none' : 'grid';
  if (chevron) chevron.style.transform = _doneCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)';
}

function renderTasks(tasks) {
  const el = document.getElementById('taskList');
  if (!tasks.length) {
    el.innerHTML = emptyStateHTML('📋', 'No tasks yet', 'Stay productive — add your first task to get started!', '+ New Task', "document.getElementById('openTaskModal').click()");
    return;
  }

  // Split into active and done
  const doneTasks   = tasks.filter(t => t.status === 'DONE' || t.status === 'COMPLETED');
  const activeTasks = tasks.filter(t => t.status !== 'DONE' && t.status !== 'COMPLETED');

  // Group active tasks by section (null/empty → "General")
  const groups = {};
  activeTasks.forEach(t => {
    const sec = (t.section && t.section.trim()) ? t.section.trim() : 'General';
    if (!groups[sec]) groups[sec] = [];
    groups[sec].push(t);
  });

  // Sort active tasks within each section: IN_PROGRESS before PENDING, then priority, then title
  const STATUS_SORT = { IN_PROGRESS: 0, PENDING: 1, CANCELLED: 2 };
  Object.values(groups).forEach(arr => arr.sort((a, b) => {
    const sa = STATUS_SORT[a.status] ?? 1;
    const sb = STATUS_SORT[b.status] ?? 1;
    if (sa !== sb) return sa - sb;
    const pa = PRIORITY_ORDER[a.priority] ?? 4;
    const pb = PRIORITY_ORDER[b.priority] ?? 4;
    return pa !== pb ? pa - pb : (a.title || '').localeCompare(b.title || '');
  }));

  // Sort sections: General last
  const sectionKeys = Object.keys(groups).sort((a, b) => {
    if (a === 'General') return 1;
    if (b === 'General') return -1;
    return a.localeCompare(b);
  });

  // Sort done tasks by priority then title
  doneTasks.sort((a, b) => {
    const pa = PRIORITY_ORDER[a.priority] ?? 4;
    const pb = PRIORITY_ORDER[b.priority] ?? 4;
    return pa !== pb ? pa - pb : (a.title || '').localeCompare(b.title || '');
  });

  let html = '';

  // Active sections
  if (sectionKeys.length) {
    html += sectionKeys.map(sec => `
      <div class="task-section-group">
        ${sectionKeys.length > 1 || sec !== 'General'
          ? `<div class="task-section-label">${esc(sec)}</div>`
          : ''}
        <div class="card-grid">
          ${groups[sec].map(t => renderTaskCard(t)).join('')}
        </div>
      </div>
    `).join('');
  } else if (!doneTasks.length) {
    el.innerHTML = emptyStateHTML('📋', 'No tasks yet', 'Stay productive — add your first task to get started!', '+ New Task', "document.getElementById('openTaskModal').click()");
    return;
  }

  // Done section — collapsible, at the bottom
  if (doneTasks.length) {
    html += `
      <div class="task-section-group task-done-section">
        <div class="task-section-label done-section-header" onclick="toggleDoneSection()" style="cursor:pointer;user-select:none;display:flex;align-items:center;justify-content:space-between;">
          <span>✅ Done <span class="done-count-badge" style="background:rgba(108,99,255,0.2);color:#a0a0c0;border-radius:12px;padding:1px 8px;font-size:0.78rem;font-weight:600;margin-left:6px;">${doneTasks.length}</span></span>
          <span id="doneSectionChevron" style="transition:transform 0.2s;display:inline-block;${_doneCollapsed ? 'transform:rotate(-90deg)' : ''}">▾</span>
        </div>
        <div id="doneSectionBody" class="card-grid" style="display:${_doneCollapsed ? 'none' : 'grid'}">
          ${doneTasks.map(t => renderTaskCard(t)).join('')}
        </div>
      </div>
    `;
  }

  el.innerHTML = html;
}

function renderTaskCard(t) {
  const statusClass = { PENDING: 'pending', IN_PROGRESS: 'progress', DONE: 'done', COMPLETED: 'done' }[t.status] || 'pending';
  const statusLabel = { PENDING: '⭕ Pending', IN_PROGRESS: '🔄 In Progress', DONE: '✅ Done', COMPLETED: '✅ Done', CANCELLED: '🚫 Cancelled' }[t.status] || t.status;
  const isDone = t.status === 'DONE' || t.status === 'COMPLETED';

  return `
    <div class="card" data-id="${t.id}">
      <div class="card-title">
        <span style="${isDone ? 'text-decoration:line-through;opacity:0.6' : ''}">${esc(t.title)}</span>
        ${badgePriority(t.priority)}
      </div>
      ${t.description ? `<p class="card-desc">${esc(t.description)}</p>` : ''}
      <div class="card-meta">
        ${t.dueDate ? `<span>📅 ${formatDate(t.dueDate)}</span>` : ''}
        ${t.alarmTime && !t.alarmFired ? `<span title="Alarm set: ${t.alarmTime.slice(0,16)}">⏰ ${t.alarmTime.slice(11,16)}</span>` : ''}
        ${t.alarmTime &&  t.alarmFired ? `<span style="opacity:0.45" title="Alarm already fired">⏰ fired</span>` : ''}
        ${t.category ? `<span>🏷 ${esc(t.category)}</span>` : ''}
        ${t.goalId ? `<span>🎯 Goal linked</span>` : ''}
        ${t.collaboratorIds && t.collaboratorIds.length ? `<span>👥 ${t.collaboratorIds.length} collab(s)</span>` : ''}
      </div>
      ${t.subTasks && t.subTasks.length ? `
        <div class="subtask-collapse">
          <button class="subtask-toggle-btn" onclick="toggleSubtasks('${t.id}')">
            🤖 AI Breakdown <span class="subtask-count">(${t.subTasks.length})</span>
            <span id="stc-${t.id}" class="subtask-chevron">▾</span>
          </button>
          <ul class="subtask-list" id="stl-${t.id}" style="display:none">
            ${t.subTasks.map(s => `<li class="subtask-item">${esc(s)}</li>`).join('')}
          </ul>
        </div>` : ''}
      <div class="card-actions">
        <button class="btn-status ${statusClass}" onclick="cycleStatus('${t.id}', '${t.status}')">${statusLabel}</button>
        <button class="btn-icon" onclick="openEditTask('${t.id}')" title="Edit task">✏️</button>
        <button class="btn-icon" onclick="openTaskCollab('${t.id}')" title="Collaborators">👥</button>
        ${!isDone ? `<button class="btn-icon" onclick="generateBreakdown('${t.id}')">🤖 AI</button>` : ''}
        <button class="btn-icon" onclick="deleteTask('${t.id}')">🗑</button>
      </div>
    </div>
  `;
}

async function cycleStatus(id, current) {
  const cycle = { PENDING: 'IN_PROGRESS', IN_PROGRESS: 'DONE', DONE: 'PENDING', COMPLETED: 'PENDING' };
  const next = cycle[current] || 'IN_PROGRESS';

  // Optimistic in-place update — no page reload
  const taskIdx = allTasks.findIndex(t => t.id === id);
  if (taskIdx !== -1) allTasks[taskIdx].status = next;

  // Re-render the single card immediately
  const cardEl = document.querySelector(`.card[data-id="${id}"]`);
  if (cardEl && taskIdx !== -1) {
    cardEl.outerHTML = renderTaskCard(allTasks[taskIdx]);
  }

  try {
    await apiPatch(`/api/tasks/${id}/status`, { status: next });
  } catch (err) {
    // Fallback to PUT
    try {
      await apiPut(`/api/tasks/${id}`, { title: allTasks[taskIdx]?.title || 'Task', status: next });
    } catch (err2) {
      // Revert optimistic update on failure
      if (taskIdx !== -1) allTasks[taskIdx].status = current;
      renderTasks(getFilteredTasks());
      toast('Failed to update status', 'error');
    }
  }
}

async function generateBreakdown(id) {
  toast('Generating AI breakdown…');
  try {
    await apiPost(`/api/tasks/${id}/breakdown`, {});
    toast('AI breakdown added!');
    loadTasks();
  } catch (err) { toast(err.message, 'error'); }
}

function toggleSubtasks(taskId) {
  const list    = el(`stl-${taskId}`);
  const chevron = el(`stc-${taskId}`);
  if (!list) return;
  const isOpen = list.style.display !== 'none';
  list.style.display = isOpen ? 'none' : 'block';
  if (chevron) chevron.textContent = isOpen ? '▾' : '▴';
}

async function deleteTask(id) {
  if (!confirm('Delete this task?')) return;
  try {
    await apiDelete(`/api/tasks/${id}`);
    toast('Task deleted');
    loadTasks();
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// GOALS
// ==========================================================================

document.getElementById('openGoalModal').addEventListener('click', () => openModal('goalModal'));

document.getElementById('goalForm').addEventListener('submit', async e => {
  e.preventDefault();
  const editId = document.getElementById('goalEditId').value;
  const payload = {
    title:      document.getElementById('goalTitle').value,
    description: document.getElementById('goalDesc').value,
    category:   document.getElementById('goalCategory').value,
    targetDate: document.getElementById('goalTarget').value || null
  };
  try {
    if (editId) {
      await apiPut(`/api/goals/${editId}`, payload);
      toast('Goal updated!');
    } else {
      await apiPost('/api/goals', payload);
      toast('Goal created!');
      aiSuggest(`New goal set: "${payload.title}". Give a short motivational tip for achieving it.`);
    }
    document.getElementById('goalModal').classList.add('hidden');
    document.getElementById('goalForm').reset();
    document.getElementById('goalEditId').value = '';
    document.getElementById('goalModalTitle').textContent = 'New Goal';
    document.getElementById('goalSubmitBtn').textContent = 'Create Goal';
    loadGoals();
  } catch (err) { toast(err.message, 'error'); }
});

async function loadGoals() {
  document.getElementById('goalList').innerHTML = '<div class="loading">Loading goals…</div>';
  try {
    const res = await apiGet('/api/goals');
    allGoals = res.data || [];
    renderGoals(allGoals);
  } catch (err) { toast(err.message, 'error'); }
}

function renderGoals(goals) {
  const el = document.getElementById('goalList');
  if (!goals.length) { el.innerHTML = emptyStateHTML('🎯', 'No goals yet', 'Set a goal and track your progress over time.', '+ New Goal', "openModal('goalModal')"); return; }

  el.innerHTML = goals.map(g => `
    <div class="card">
      <div class="card-title">
        ${esc(g.title)}
        <span class="badge badge-pending">${esc(g.category || '')}</span>
      </div>
      ${g.description ? `<p class="card-desc">${esc(g.description)}</p>` : ''}
      <div class="card-meta">
        <span>${esc(g.status || '')}</span>
        ${g.targetDate ? `<span>🎯 ${formatDate(g.targetDate)}</span>` : ''}
      </div>
      <div class="progress-bar-wrap">
        <div class="progress-bar-fill" style="width:${g.progressPercent || 0}%"></div>
      </div>
      <p style="font-size:0.75rem;color:var(--text-muted);margin-top:0.25rem">${g.progressPercent || 0}% complete</p>
      ${g.aiInsight ? `<div class="ai-insight">🤖 ${esc(g.aiInsight)}</div>` : ''}
      <div class="card-actions">
        <button class="btn-icon" onclick="openEditGoal('${g.id}')">✏️ Edit</button>
        <button class="btn-icon" onclick="openGoalCollab('${g.id}')">👥 Collab</button>
        <button class="btn-icon" onclick="analyzeGoal('${g.id}')">🤖 AI Analyze</button>
        <button class="btn-icon" onclick="deleteGoal('${g.id}')">🗑 Delete</button>
      </div>
    </div>
  `).join('');
}

async function analyzeGoal(id) {
  toast('Analyzing goal with AI…');
  try {
    await apiPost(`/api/goals/${id}/analyze`, {});
    toast('AI insight added!');
    loadGoals();
  } catch (err) { toast(err.message, 'error'); }
}
async function deleteGoal(id) {
  if (!confirm('Delete this goal?')) return;
  try {
    await apiDelete(`/api/goals/${id}`);
    toast('Goal deleted');
    loadGoals();
  } catch (err) { toast(err.message, 'error'); }
}

async function openEditTask(id) {
  const task = allTasks.find(t => t.id === id);
  if (!task) { toast('Task not found', 'error'); return; }

  // Populate dropdowns first (same as openTaskModal)
  const sel = document.getElementById('taskGoalId');
  if (allGoals.length === 0) {
    try { const r = await apiGet('/api/goals'); allGoals = r.data || []; } catch (_) {}
  }
  sel.innerHTML = '<option value="">— No goal —</option>' +
    allGoals.map(g => `<option value="${g.id}">${esc(g.title)}</option>`).join('');
  try {
    const cr = await apiGet('/api/fitness/categories');
    const cats = cr.data || [];
    const catSel = document.getElementById('taskFitnessCategoryId');
    catSel.innerHTML = '<option value="">— None —</option>' +
      cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  } catch (_) {}
  try {
    const tr = await apiGet('/api/tasks');
    const tasks = (tr.data || []).filter(t => t.id !== id);
    const parentSel = document.getElementById('taskParentId');
    parentSel.innerHTML = '<option value="">— Top-level task —</option>' +
      tasks.map(t => `<option value="${t.id}">${esc(t.title)}</option>`).join('');
  } catch (_) {}

  // Fill in fields
  document.getElementById('taskEditId').value        = task.id;
  document.getElementById('taskTitle').value          = task.title || '';
  document.getElementById('taskDesc').value           = task.description || '';
  document.getElementById('taskPriority').value       = task.priority || 'MEDIUM';
  document.getElementById('taskDue').value            = task.dueDate ? task.dueDate.slice(0,16) : '';
  document.getElementById('taskCategory').value       = task.category || '';
  document.getElementById('taskSection').value        = task.section || '';
  document.getElementById('taskGoalId').value         = task.goalId || '';
  document.getElementById('taskScheduledDate').value  = task.scheduledDate || '';
  document.getElementById('taskFitnessCategoryId').value = task.fitnessCategoryId || '';
  document.getElementById('taskCaloriesConsumed').value  = task.caloriesConsumed ?? '';
  document.getElementById('taskCaloriesBurned').value    = task.caloriesBurned ?? '';
  document.getElementById('caloriesRow').style.display  = task.fitnessCategoryId ? 'flex' : 'none';
  document.getElementById('taskParentId').value       = task.parentTaskId || '';
  document.getElementById('taskDuration').value       = task.durationMinutes ?? '';
  document.getElementById('taskAutoComplete').checked = task.autoComplete || false;
  // Recurring fields
  const recCb = document.getElementById('taskRecurring');
  recCb.checked = task.recurring || false;
  document.getElementById('taskRecurringTime').value = task.recurringTime || '08:00';
  document.getElementById('recurringTimeGroup').style.display = task.recurring ? 'block' : 'none';
  document.getElementById('taskAlarmTime').value = task.alarmTime ? task.alarmTime.slice(0,16) : '';

  // Switch modal to edit mode
  document.getElementById('taskModalTitle').textContent  = 'Edit Task';
  document.getElementById('taskSubmitBtn').textContent   = 'Save Changes';
  openModal('taskModal');
}

async function openEditGoal(id) {
  const goal = allGoals.find(g => g.id === id);
  if (!goal) { toast('Goal not found', 'error'); return; }

  document.getElementById('goalEditId').value   = goal.id;
  document.getElementById('goalTitle').value    = goal.title || '';
  document.getElementById('goalDesc').value     = goal.description || '';
  document.getElementById('goalCategory').value = goal.category || 'OTHER';
  document.getElementById('goalTarget').value   = goal.targetDate || '';

  document.getElementById('goalModalTitle').textContent = 'Edit Goal';
  document.getElementById('goalSubmitBtn').textContent  = 'Save Changes';
  openModal('goalModal');
}

async function openTaskCollab(id) {
  const task = allTasks.find(t => t.id === id);
  if (!task) { toast('Task not found', 'error'); return; }
  const email = prompt(`Add collaborator to "${esc(task.title)}"\nEnter their email address:`);
  if (!email || !email.trim()) return;
  try {
    await apiPost(`/api/tasks/${id}/collaborators`, { email: email.trim().toLowerCase() });
    toast('Collaborator added! They can now see this task.');
    loadTasks();
  } catch (err) { toast(err.message || 'Failed to add collaborator', 'error'); }
}

async function openGoalCollab(id) {
  const goal = allGoals.find(g => g.id === id);
  if (!goal) { toast('Goal not found', 'error'); return; }
  const email = prompt(`Add collaborator to goal "${esc(goal.title)}"\nEnter their email address:`);
  if (!email || !email.trim()) return;
  try {
    await apiPost(`/api/goals/${id}/collaborators`, { email: email.trim().toLowerCase() });
    toast('Collaborator added! They can now see this goal.');
    loadGoals();
  } catch (err) { toast(err.message || 'Failed to add collaborator', 'error'); }
}

// ==========================================================================
// FITNESS — CALORIE SUMMARY
// ==========================================================================

async function loadCalorieSummary() {
  const date = document.getElementById('fitnessSummaryDate').value;
  if (!date) { toast('Please select a date first.', 'error'); return; }
  try {
    const res = await apiGet(`/api/tasks/calories/summary?date=${date}`);
    const d   = res.data || res;
    const consumed = d.totalConsumed || 0;
    const burned   = d.totalBurned   || 0;
    const net      = d.net           != null ? d.net : (consumed - burned);
    document.getElementById('calConsumedDisplay').textContent = `Consumed: ${consumed} kcal`;
    document.getElementById('calBurnedDisplay').textContent   = `Burned: ${burned} kcal`;
    const netEl = document.getElementById('calNetDisplay');
    netEl.textContent = `Net: ${net} kcal`;
    netEl.style.color = net > 0 ? '#e07b54' : net < 0 ? '#4caf96' : '';
    document.getElementById('calorieSummaryBar').style.display = 'flex';
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// PERFORMANCE SYSTEM — FITNESS MODULE
// ==========================================================================

// ---- State ----
let _fitSelectedDate = new Date().toISOString().slice(0, 10);
let _fitProfile      = null;
let _activeWorkouts  = [];
let _completedVisible = false;
let _calcOpen        = false;

// ---- Element helper ----
function el(id) { return document.getElementById(id); }

// ---- FAB visibility ----
function showFitFab() {
  const fab = el('fitAddWorkoutFab');
  if (fab) fab.classList.add('visible');
}
function hideFitFab() {
  const fab = el('fitAddWorkoutFab');
  if (fab) fab.classList.remove('visible');
}

// ==========================================================================
// TAB INIT
// ==========================================================================
function initFitnessTab() {
  renderDateScroller();
  loadFitnessProfile().then(() => loadFitnessSummary(_fitSelectedDate));
  loadActiveWorkouts();
  initScheduleSection();
  showFitFab();
}

// ==========================================================================
// DATE SCROLLER
// ==========================================================================
function renderDateScroller() {
  const scroller = el('fitDateScroll');
  if (!scroller) return;
  const today = new Date();
  const DAYS  = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const items = [];
  for (let i = -3; i <= 3; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    const str      = d.toISOString().slice(0, 10);
    const isActive = str === _fitSelectedDate;
    items.push(`
      <div class="fit-date-item${isActive ? ' active' : ''}" onclick="selectFitDate('${str}')">
        <span class="fit-date-dow">${DAYS[d.getDay()]}</span>
        <span class="fit-date-num">${d.getDate()}</span>
      </div>`);
  }
  scroller.innerHTML = items.join('');
  // Scroll to center (active item)
  const active = scroller.querySelector('.active');
  if (active) active.scrollIntoView({ inline: 'center', behavior: 'smooth' });
}

function selectFitDate(dateStr) {
  _fitSelectedDate = dateStr;
  renderDateScroller();
  loadFitnessSummary(dateStr);
}

// ==========================================================================
// FITNESS PROFILE
// ==========================================================================
async function loadFitnessProfile() {
  try {
    const res = await apiGet('/api/fitness/profile');
    _fitProfile = res.data || null;
    if (_fitProfile && _fitProfile.dailyCalorieGoal > 0 && el('fitSumGoal')) {
      el('fitSumGoal').textContent = _fitProfile.dailyCalorieGoal;
    }
  } catch (_) {}
}

function openProfileEdit() {
  if (_fitProfile) {
    if (el('profAge'))      el('profAge').value      = _fitProfile.age      || '';
    if (el('profWeight'))   el('profWeight').value   = _fitProfile.weightKg || '';
    if (el('profHeight'))   el('profHeight').value   = _fitProfile.heightCm || '';
    if (el('profGender'))   el('profGender').value   = _fitProfile.gender   || 'M';
    if (el('profActivity')) el('profActivity').value = _fitProfile.activityLevel || 'MODERATE';
    if (el('profGoal'))     el('profGoal').value     = _fitProfile.dailyCalorieGoal || '';
  }
  openModal('fitProfileModal');
}

async function saveFitnessProfile(e) {
  e.preventDefault();
  try {
    const res = await apiPut('/api/fitness/profile', {
      age:              parseInt(el('profAge').value)      || 0,
      weightKg:         parseFloat(el('profWeight').value) || 0,
      heightCm:         parseFloat(el('profHeight').value) || 0,
      gender:           el('profGender').value,
      activityLevel:    el('profActivity').value,
      dailyCalorieGoal: parseInt(el('profGoal').value)     || 0
    });
    _fitProfile = res.data;
    el('fitProfileModal').classList.add('hidden');
    toast('Profile saved! ✅', 'success');
    loadFitnessSummary(_fitSelectedDate);
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// DAILY SUMMARY + PROGRESS RING
// ==========================================================================
async function loadFitnessSummary(dateStr) {
  try {
    const res      = await apiGet(`/api/calories/summary?date=${dateStr}`);
    const d        = res.data || {};
    const consumed = d.consumed || 0;
    const goal     = (_fitProfile && _fitProfile.dailyCalorieGoal > 0)
                     ? _fitProfile.dailyCalorieGoal : 2000;
    const remaining = Math.max(0, goal - consumed);

    if (el('fitSumConsumed'))  el('fitSumConsumed').textContent  = consumed;
    if (el('fitSumGoal'))      el('fitSumGoal').textContent      = goal;

    const remEl = el('fitSumRemaining');
    if (remEl) {
      if (consumed > goal) {
        remEl.className     = 'fit-sum-val over-goal';
        remEl.textContent   = (consumed - goal) + ' over';
      } else {
        remEl.className     = 'fit-sum-val remaining';
        remEl.textContent   = remaining;
      }
    }
    drawProgressRing(consumed, goal);
  } catch (_) {
    drawProgressRing(0, 2000);
  }
}

function drawProgressRing(consumed, goal) {
  const circumference = 301.6; // 2π × 48
  const pct    = goal > 0 ? Math.min(consumed / goal, 1) : 0;
  const offset = circumference * (1 - pct);
  const fillEl = el('fitRingFill');
  const pctEl  = el('fitRingPct');
  if (fillEl) {
    fillEl.style.strokeDashoffset = offset;
    fillEl.style.stroke = pct >= 1 ? '#f87171' : pct >= 0.75 ? '#fbbf24' : '#6c63ff';
  }
  if (pctEl) pctEl.textContent = Math.round(pct * 100) + '%';
}

// ---- Quick Calorie Log ----
function toggleQuickLog() {
  const row = el('fitQuickLogRow');
  const btn = row ? row.nextElementSibling : null;
  if (!row) return;
  const isVisible = row.style.display !== 'none';
  row.style.display = isVisible ? 'none' : 'flex';
  if (btn) btn.style.display = isVisible ? '' : 'none';
}

async function quickLogCalories() {
  const consumed = parseInt(el('fitQuickConsumed')?.value) || 0;
  const burned   = parseInt(el('fitQuickBurned')?.value)   || 0;
  const mealType = el('fitQuickMeal')?.value || 'SNACK';
  if (!consumed && !burned) { toast('Enter at least one value', 'error'); return; }
  try {
    await apiPost('/api/calories', { date: _fitSelectedDate, consumed, burned, mealType });
    if (el('fitQuickConsumed')) el('fitQuickConsumed').value = '';
    if (el('fitQuickBurned'))   el('fitQuickBurned').value   = '';
    toast('Calories logged! ✅', 'success');
    toggleQuickLog();
    loadFitnessSummary(_fitSelectedDate);
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// CALORIE CALCULATOR
// ==========================================================================
function toggleCalcForm() {
  _calcOpen = !_calcOpen;
  const body    = el('fitCalcBody');
  const chevron = el('fitCalcChevron');
  if (body)    body.style.display    = _calcOpen ? 'block' : 'none';
  if (chevron) chevron.classList.toggle('open', _calcOpen);
  if (_calcOpen && _fitProfile) {
    if (el('calcAge'))      el('calcAge').value      = _fitProfile.age      || '';
    if (el('calcWeight'))   el('calcWeight').value   = _fitProfile.weightKg || '';
    if (el('calcHeight'))   el('calcHeight').value   = _fitProfile.heightCm || '';
    if (el('calcGender'))   el('calcGender').value   = _fitProfile.gender   || 'M';
    if (el('calcActivity')) el('calcActivity').value = _fitProfile.activityLevel || 'MODERATE';
  }
}

async function calculateAndSave(e) {
  e.preventDefault();
  const age      = parseInt(el('calcAge').value);
  const weight   = parseFloat(el('calcWeight').value);
  const height   = parseFloat(el('calcHeight').value);
  const gender   = el('calcGender').value;
  const activity = el('calcActivity').value;
  if (!age || !weight || !height) { toast('Fill all fields', 'error'); return; }

  // Mifflin-St Jeor formula
  let bmr  = 10 * weight + 6.25 * height - 5 * age;
  bmr     += (gender === 'M') ? 5 : -161;
  const multipliers = { SEDENTARY: 1.2, LIGHT: 1.375, MODERATE: 1.55, ACTIVE: 1.725, VERY_ACTIVE: 1.9 };
  const tdee = Math.round(bmr * (multipliers[activity] || 1.55));

  if (el('fitCalcResultVal')) el('fitCalcResultVal').textContent = tdee;
  if (el('fitCalcResult'))    el('fitCalcResult').style.display   = 'block';

  try {
    const res = await apiPut('/api/fitness/profile', {
      age, weightKg: weight, heightCm: height,
      gender, activityLevel: activity, dailyCalorieGoal: tdee
    });
    _fitProfile = res.data;
    toast(`Goal set: ${tdee} kcal/day ✅`, 'success');
    loadFitnessSummary(_fitSelectedDate);
  } catch (err) { toast('Saved locally. ' + err.message); }
}

// ==========================================================================
// WORKOUT LIST
// ==========================================================================
async function loadActiveWorkouts() {
  const listEl = el('activeWorkoutsList');
  if (!listEl) return;
  try {
    const res      = await apiGet('/api/workouts?archived=false');
    _activeWorkouts = res.data || [];
    const countEl  = el('activeWorkoutsCount');
    if (countEl) countEl.textContent = _activeWorkouts.length;
    if (!_activeWorkouts.length) {
      listEl.innerHTML = emptyStateHTML('💪', 'No active workouts', 'Start a workout routine and track your fitness journey!', '+ Add Workout', 'createNewWorkout()');
      return;
    }
    listEl.innerHTML = _activeWorkouts.map(w => renderWorkoutCard(w)).join('');
  } catch (err) {
    if (listEl) listEl.innerHTML = `<p class="loading">${esc(err.message)}</p>`;
  }
}

async function loadCompletedWorkouts() {
  const listEl = el('completedWorkoutsList');
  if (!listEl) return;
  listEl.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res      = await apiGet('/api/workouts/all');
    const all      = res.data || [];
    const completed = all.filter(w => w.status === 'COMPLETED' || w.archived);
    if (!completed.length) {
      listEl.innerHTML = '<p class="loading">No completed workouts yet.</p>';
      return;
    }
    listEl.innerHTML = completed.map(w => renderWorkoutCard(w, true)).join('');
  } catch (err) {
    if (listEl) listEl.innerHTML = `<p class="loading">${esc(err.message)}</p>`;
  }
}

function toggleCompletedSection() {
  _completedVisible = !_completedVisible;
  const listEl    = el('completedWorkoutsList');
  const chevronEl = el('completedChevron');
  if (listEl) {
    listEl.style.display = _completedVisible ? 'flex' : 'none';
    if (_completedVisible) { listEl.style.flexDirection = 'column'; listEl.style.gap = '0.75rem'; }
  }
  if (chevronEl) chevronEl.classList.toggle('open', _completedVisible);
  if (_completedVisible) loadCompletedWorkouts();
}

// ==========================================================================
// WEEKLY EXERCISE SCHEDULE
// ==========================================================================
let _schedCurrentWeekStart = null; // ISO string "YYYY-MM-DD" (always Monday)
let _schedMode             = 'custom'; // 'custom' | 'ai'
let _schedData             = null; // WorkoutSchedule from API
let _schedMonthlyVisible   = false;
let _schedCurrentMonth     = null; // { year, month } 1-based

/** Returns ISO date string for the Monday of the given date. */
function schedMonday(dateStr) {
  const d = new Date(dateStr + 'T00:00:00');
  const day = d.getDay(); // 0=Sun
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return d.toISOString().slice(0, 10);
}

/** Human-readable label: "Mon Feb 24 – Sun Mar 2" */
function schedWeekRangeLabel(mondayStr) {
  const mon = new Date(mondayStr + 'T00:00:00');
  const sun = new Date(mon); sun.setDate(mon.getDate() + 6);
  const fmt = d => d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  return `${fmt(mon)} – ${fmt(sun)}`;
}

function initScheduleSection() {
  _schedCurrentWeekStart = schedMonday(new Date().toISOString().slice(0, 10));
  const now = new Date();
  _schedCurrentMonth = { year: now.getFullYear(), month: now.getMonth() + 1 };
  setSchedMode('custom');
  loadSchedule();
}

/** Toggle between 'custom' (week grid) and 'ai' (AI prompt) modes. */
function setSchedMode(mode) {
  _schedMode = mode;
  document.querySelectorAll('.sched-mode-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.mode === mode);
  });
  const aiPanel     = el('schedAiPanel');
  const customPanel = el('schedCustomPanel');
  if (aiPanel)     aiPanel.style.display     = mode === 'ai'     ? 'block' : 'none';
  if (customPanel) customPanel.style.display = mode === 'custom' ? 'block' : 'none';
}

function schedWeekPrev() {
  const d = new Date(_schedCurrentWeekStart + 'T00:00:00');
  d.setDate(d.getDate() - 7);
  _schedCurrentWeekStart = d.toISOString().slice(0, 10);
  loadSchedule();
}

function schedWeekNext() {
  const d = new Date(_schedCurrentWeekStart + 'T00:00:00');
  d.setDate(d.getDate() + 7);
  _schedCurrentWeekStart = d.toISOString().slice(0, 10);
  loadSchedule();
}

async function loadSchedule() {
  const labelEl = el('schedWeekLabel');
  if (labelEl) labelEl.textContent = schedWeekRangeLabel(_schedCurrentWeekStart);
  const gridEl = el('schedWeekGrid');
  if (gridEl) gridEl.innerHTML = '<div style="text-align:center;padding:1rem;color:var(--text-muted)">Loading…</div>';
  try {
    const res = await apiGet(`/api/workout-schedule?weekStart=${_schedCurrentWeekStart}`);
    _schedData = res.data;
    renderWeekGrid(_schedData);
  } catch (err) {
    if (gridEl) gridEl.innerHTML = `<p style="color:var(--text-muted);padding:1rem">${esc(err.message)}</p>`;
  }
}

function renderWeekGrid(data) {
  const gridEl = el('schedWeekGrid');
  if (!gridEl) return;
  const days = data && data.days ? data.days : [];
  const todayStr = new Date().toISOString().slice(0, 10);
  gridEl.innerHTML = days.map(day => renderDayCol(day, todayStr)).join('');
}

function renderDayCol(day, todayStr) {
  const DOW_SHORT  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const DOW_ORDER  = {MONDAY:0,TUESDAY:1,WEDNESDAY:2,THURSDAY:3,FRIDAY:4,SATURDAY:5,SUNDAY:6};
  const label = DOW_SHORT[DOW_ORDER[day.dayOfWeek]] ?? day.dayOfWeek;
  const dateNum = day.date ? new Date(day.date + 'T00:00:00').getDate() : '';
  const isToday = day.date === todayStr;
  const workouts = day.workouts || [];
  const pwHtml = workouts.map(pw => renderPlannedWorkout(pw, day.dayOfWeek)).join('');
  return `
<div class="sched-day-col${isToday ? ' today-col' : ''}">
  <div class="sched-day-header">
    <div>${label}</div>
    <div class="sched-day-date">${dateNum}</div>
  </div>
  <div class="sched-day-body" id="sched-day-${day.dayOfWeek}">
    ${pwHtml}
    <button class="sched-add-btn" onclick="addPlannedWorkout('${day.dayOfWeek}')">+ Add</button>
  </div>
</div>`;
}

function renderPlannedWorkout(pw, dayOfWeek) {
  const exCount = (pw.exercises || []).length;
  return `
<div class="sched-planned-workout" id="spw-${pw.planId}">
  <div class="sched-pw-name" title="${esc(pw.name)}">${esc(pw.name)}</div>
  <div class="sched-pw-meta">${pw.type || ''} ${pw.durationMinutes ? pw.durationMinutes + 'min' : ''}</div>
  ${exCount ? `<div class="sched-pw-meta">${exCount} exercise${exCount>1?'s':''}</div>` : ''}
  <div class="sched-pw-actions">
    <button class="sched-pw-btn" onclick="toggleSchedExList('${pw.planId}')">👁 Exercises</button>
    <button class="sched-pw-btn delete" onclick="deletePlannedWorkout('${dayOfWeek}','${pw.planId}')">🗑</button>
  </div>
  <ul class="sched-ex-list" id="scex-${pw.planId}" style="display:none">
    ${(pw.exercises || []).map(e => `<li>${esc(e.name)}${e.sets ? ' ' + e.sets + '×' + (e.reps||'?') : ''}</li>`).join('') || '<li>No exercises</li>'}
  </ul>
</div>`;
}

function toggleSchedExList(planId) {
  const ul = el(`scex-${planId}`);
  if (ul) ul.style.display = ul.style.display === 'none' ? 'block' : 'none';
}

function addPlannedWorkout(dayOfWeek) {
  const dayBody = el(`sched-day-${dayOfWeek}`);
  if (!dayBody || dayBody.querySelector('.sched-add-form')) return;
  const addBtn = dayBody.querySelector('.sched-add-btn');
  const form = document.createElement('div');
  form.className = 'sched-add-form';
  form.id = `sched-add-form-${dayOfWeek}`;
  form.innerHTML = `
    <input type="text" id="spw-name-${dayOfWeek}" placeholder="Workout name *" style="font-weight:600"/>
    <select id="spw-type-${dayOfWeek}">
      <option value="">— Type —</option>
      <option value="STRENGTH">Strength</option>
      <option value="CARDIO">Cardio</option>
      <option value="FLEXIBILITY">Flexibility</option>
      <option value="HIIT">HIIT</option>
      <option value="SPORT">Sport</option>
      <option value="OTHER">Other</option>
    </select>
    <input type="number" id="spw-dur-${dayOfWeek}" placeholder="Duration (min)" min="1"/>
    <div class="sched-add-form-btns">
      <button class="cancel-btn" onclick="cancelPlannedWorkout('${dayOfWeek}')">Cancel</button>
      <button class="save-btn"   onclick="savePlannedWorkout('${dayOfWeek}')">Save</button>
    </div>`;
  if (addBtn) dayBody.insertBefore(form, addBtn); else dayBody.appendChild(form);
  el(`spw-name-${dayOfWeek}`)?.focus();
}

function cancelPlannedWorkout(dayOfWeek) {
  const form = el(`sched-add-form-${dayOfWeek}`);
  if (form) form.remove();
}

async function savePlannedWorkout(dayOfWeek) {
  const name = el(`spw-name-${dayOfWeek}`)?.value?.trim();
  if (!name) { toast('Enter a workout name', 'error'); return; }
  const type = el(`spw-type-${dayOfWeek}`)?.value || undefined;
  const dur  = parseInt(el(`spw-dur-${dayOfWeek}`)?.value) || undefined;

  if (!_schedData) _schedData = { weekStartDate: _schedCurrentWeekStart, days: [] };
  let day = (_schedData.days || []).find(d => d.dayOfWeek === dayOfWeek);
  if (!day) {
    const DOW_OFFSET = {MONDAY:0,TUESDAY:1,WEDNESDAY:2,THURSDAY:3,FRIDAY:4,SATURDAY:5,SUNDAY:6};
    const base = new Date(_schedCurrentWeekStart + 'T00:00:00');
    base.setDate(base.getDate() + (DOW_OFFSET[dayOfWeek] ?? 0));
    day = { dayOfWeek, date: base.toISOString().slice(0, 10), workouts: [] };
    _schedData.days.push(day);
  }
  day.workouts = day.workouts || [];
  day.workouts.push({ name, type, durationMinutes: dur, exercises: [], notes: '' });

  try {
    const res = await apiPut('/api/workout-schedule', {
      weekStartDate: _schedCurrentWeekStart,
      days: _schedData.days
    });
    _schedData = res.data;
    renderWeekGrid(_schedData);
    toast('Workout added to schedule ✅', 'success');
  } catch (err) { toast(err.message, 'error'); }
}

async function deletePlannedWorkout(dayOfWeek, planId) {
  if (!_schedData) return;
  const day = (_schedData.days || []).find(d => d.dayOfWeek === dayOfWeek);
  if (!day) return;
  day.workouts = (day.workouts || []).filter(pw => pw.planId !== planId);
  try {
    const res = await apiPut('/api/workout-schedule', {
      weekStartDate: _schedCurrentWeekStart, days: _schedData.days
    });
    _schedData = res.data;
    renderWeekGrid(_schedData);
  } catch (err) { toast(err.message, 'error'); }
}

async function pushWeekToActive() {
  try {
    const res = await apiPost(`/api/workout-schedule/push?weekStart=${_schedCurrentWeekStart}`, {});
    const msg = res.data?.message || 'Pushed to active workouts';
    toast('✅ ' + msg, 'success');
    loadActiveWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

function toggleSchedMonthly() {
  _schedMonthlyVisible = !_schedMonthlyVisible;
  const panel = el('schedMonthPanel');
  if (panel) panel.style.display = _schedMonthlyVisible ? 'block' : 'none';
  if (_schedMonthlyVisible) renderMonthGrid();
}

function schedMonthPrev() {
  _schedCurrentMonth.month--;
  if (_schedCurrentMonth.month < 1) { _schedCurrentMonth.month = 12; _schedCurrentMonth.year--; }
  renderMonthGrid();
}

function schedMonthNext() {
  _schedCurrentMonth.month++;
  if (_schedCurrentMonth.month > 12) { _schedCurrentMonth.month = 1; _schedCurrentMonth.year++; }
  renderMonthGrid();
}

async function renderMonthGrid() {
  const labelEl = el('schedMonthLabel');
  if (labelEl) {
    const d = new Date(_schedCurrentMonth.year, _schedCurrentMonth.month - 1, 1);
    labelEl.textContent = d.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  }
  const gridEl = el('schedMonthGrid');
  if (!gridEl) return;

  // Fetch all schedules to know which weeks have plans
  let weekStarts = new Set();
  try {
    const res = await apiGet('/api/workout-schedule/all');
    (res.data || []).forEach(s => weekStarts.add(s.weekStartDate));
  } catch (_) {}

  const { year, month } = _schedCurrentMonth;
  const firstDay = new Date(year, month - 1, 1);
  const lastDay  = new Date(year, month, 0);
  const todayStr = new Date().toISOString().slice(0, 10);
  const DOW_HDR  = ['Mo','Tu','We','Th','Fr','Sa','Su'];

  let html = DOW_HDR.map(d => `<div class="sched-month-day-hdr">${d}</div>`).join('');

  // Leading blanks (Mon=1)
  const startDow = firstDay.getDay() === 0 ? 7 : firstDay.getDay();
  for (let i = 1; i < startDow; i++) html += `<div></div>`;

  for (let day = 1; day <= lastDay.getDate(); day++) {
    const d = new Date(year, month - 1, day);
    const dateStr = d.toISOString().slice(0, 10);
    const monday  = schedMonday(dateStr);
    const hasPlan = weekStarts.has(monday);
    const isToday = dateStr === todayStr;
    html += `<div class="sched-month-day${hasPlan ? ' has-plan' : ''}${isToday ? ' today' : ''}"
               onclick="jumpToWeek('${monday}')" title="${dateStr}">${day}
               ${hasPlan ? '<div class="sched-dot"></div>' : ''}
             </div>`;
  }
  gridEl.innerHTML = html;
}

function jumpToWeek(mondayStr) {
  _schedCurrentWeekStart = mondayStr;
  toggleSchedMonthly(); // hide month panel
  loadSchedule();
}

async function schedAiAdd() {
  const input = el('schedAiInput');
  const prompt = input?.value?.trim();
  if (!prompt) { toast('Enter a prompt first', 'error'); return; }

  const btn = document.querySelector('.sched-ai-btn');
  if (btn) { btn.disabled = true; btn.textContent = '⏳ Planning…'; }

  try {
    const aiRes = await apiPost('/api/ai/chat', {
      message: `You are a fitness planner. The user wants: "${prompt}".
Return ONLY a JSON array of workout objects, no extra text. Each object must have:
{ "dayOfWeek": 1-7 (1=Monday), "name": string, "type": "STRENGTH|CARDIO|FLEXIBILITY|HIIT|SPORT|OTHER", "durationMinutes": number }.
Plan only the requested days. Example response: [{"dayOfWeek":1,"name":"Push Day","type":"STRENGTH","durationMinutes":60}]`
    });

    // Parse JSON from AI response text. AiChatResponse wraps the reply in message.content
    const text = aiRes.data?.message?.content
              || aiRes.data?.reply
              || (typeof aiRes.data === 'string' ? aiRes.data : '')
              || '';
    const match = text.match(/\[[\s\S]*?\]/);
    if (!match) { toast('AI did not return a valid plan. Check Groq API key.', 'error'); return; }
    const plan = JSON.parse(match[0]);

    if (!_schedData) _schedData = { weekStartDate: _schedCurrentWeekStart, days: [] };
    const base = new Date(_schedCurrentWeekStart + 'T00:00:00');

    for (const item of plan) {
      let day = (_schedData.days || []).find(d => d.dayOfWeek === item.dayOfWeek);
      if (!day) {
        const dayDate = new Date(base);
        dayDate.setDate(base.getDate() + item.dayOfWeek - 1);
        day = { dayOfWeek: item.dayOfWeek, date: dayDate.toISOString().slice(0, 10), workouts: [] };
        _schedData.days.push(day);
      }
      day.workouts = day.workouts || [];
      day.workouts.push({ name: item.name, type: item.type, durationMinutes: item.durationMinutes, exercises: [] });
    }

    const res = await apiPut('/api/workout-schedule', {
      weekStartDate: _schedCurrentWeekStart, days: _schedData.days
    });
    _schedData = res.data;
    renderWeekGrid(_schedData);
    if (input) input.value = '';
    toast('AI planned your week! ✅', 'success');
  } catch (err) {
    toast('AI plan failed: ' + err.message, 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '✨ Plan with AI'; }
  }
}

// ==========================================================================
// WORKOUT CARD RENDER
// ==========================================================================
function renderWorkoutCard(w, isArchived) {
  const statusLower = (w.status || 'draft').toLowerCase().replace('_', '_');
  const statusLabels = {
    draft: '📝 Draft', pending: '⭕ Pending',
    in_progress: '🔄 In Progress', completed: '✅ Completed', archived: '📦 Archived'
  };
  const statusLabel = statusLabels[statusLower] || w.status;

  const dateStr = w.workoutDate
    ? new Date(w.workoutDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
    : '';
  const exercises = w.exercises || [];
  const exDone    = exercises.filter(e => e.status === 'DONE').length;
  const hasCollab = w.collaboratorIds && w.collaboratorIds.length > 0;

  /* Compact preview list (read-only, status clickable) */
  const previewExHtml = exercises.length
    ? exercises.map((ex, idx) => {
        const sl = (ex.status || 'PENDING').toLowerCase();
        const sl2label = { pending: '⭕', in_progress: '🔄', done: '✅' };
        return `<div class="ex-preview-row">
          <span class="ex-preview-name">${esc(ex.name || 'Exercise ' + (idx+1))}</span>
          ${ex.sets ? `<span class="ex-preview-meta">${ex.sets}×${ex.reps||'?'} ${ex.weightKg ? ex.weightKg+'kg' : ''}</span>` : ''}
          <span class="ex-status ex-status-${sl}" style="cursor:pointer"
                onclick="cycleExerciseStatus('${w.id}',${idx},'${ex.status||'PENDING'}')"
                title="Click to cycle">${sl2label[sl] || ex.status}</span>
        </div>`;
      }).join('')
    : `<p style="font-size:0.8rem;color:var(--text-muted);margin:0">No exercises yet — click ✏️ Edit to add some.</p>`;

  /* Full exercise list in edit body */
  const exListHtml = exercises.length
    ? `<div class="ex-list" id="ex-list-${w.id}">${exercises.map((ex, idx) => renderExerciseItem(w.id, idx, ex)).join('')}</div>`
    : `<p style="font-size:0.82rem;color:var(--text-muted);padding:0 0 0.65rem">No exercises yet. Add one below!</p>`;

  return `
<div class="wk-card${isArchived ? ' wk-completed' : ''}" id="wk-${w.id}" data-id="${w.id}">
  <div class="wk-card-header" onclick="toggleExercisePreview('${w.id}')" title="Click to view exercises">
    <div class="wk-card-left">
      <span class="wk-card-name">${esc(w.name)}</span>
      <div style="display:flex;align-items:center;gap:0.4rem;flex-wrap:wrap;margin-top:0.12rem">
        <span class="wk-status wk-status-${statusLower}">${statusLabel}</span>
        ${w.type ? `<span style="font-size:0.7rem;color:var(--text-muted)">${w.type}</span>` : ''}
        ${dateStr ? `<span class="wk-card-date">${dateStr}</span>` : ''}
        ${exercises.length ? `<span style="font-size:0.7rem;color:var(--text-muted)">${exDone}/${exercises.length} done</span>` : ''}
        ${hasCollab ? `<span class="wk-collab-dot" title="${w.collaboratorIds.length} collaborator(s)"></span>` : ''}
        <span style="font-size:0.65rem;color:var(--text-muted);font-style:italic">▾ exercises</span>
      </div>
    </div>
    <div class="wk-card-right" onclick="event.stopPropagation()">
      <select class="wk-status-select" onchange="updateWorkoutStatus('${w.id}', this.value)">
        <option value="DRAFT"       ${w.status==='DRAFT'       ?'selected':''}>Draft</option>
        <option value="PENDING"     ${w.status==='PENDING'     ?'selected':''}>Pending</option>
        <option value="IN_PROGRESS" ${w.status==='IN_PROGRESS' ?'selected':''}>In Progress</option>
        <option value="COMPLETED"   ${w.status==='COMPLETED'   ?'selected':''}>Completed</option>
      </select>
      <button class="btn-icon" onclick="toggleWorkoutCard('${w.id}')" title="Edit workout">✏️</button>
      <button class="btn-icon" onclick="archiveWorkout('${w.id}')" title="${w.archived ? 'Unarchive' : 'Archive'}">📦</button>
      <button class="btn-icon" onclick="deleteWorkout('${w.id}')" title="Delete">🗑</button>
    </div>
  </div>

  <!-- Quick exercise preview (toggled by header click) -->
  <div id="ex-preview-${w.id}" class="ex-preview-panel" style="display:none">
    ${previewExHtml}
  </div>

  <!-- Full edit body (toggled by ✏️ Edit button) -->
  <div class="wk-card-body" id="wk-body-${w.id}" style="display:none">
    <div style="display:flex;gap:0.6rem;flex-wrap:wrap;margin-bottom:0.85rem">
      <input type="text" id="wk-name-${w.id}" value="${esc(w.name)}"
        style="flex:1;min-width:120px;background:var(--bg);border:1px solid var(--border);border-radius:7px;padding:0.4rem 0.65rem;color:var(--text);font-size:0.88rem;outline:none;font-family:inherit"
        placeholder="Workout name" onblur="renameWorkout('${w.id}',this.value)" onkeydown="if(event.key==='Enter')this.blur()"/>
      <select id="wk-type-${w.id}" onchange="updateWorkoutField('${w.id}')"
        style="background:var(--bg);border:1px solid var(--border);border-radius:7px;padding:0.4rem 0.5rem;color:var(--text);font-size:0.82rem;outline:none">
        <option value="">— Type —</option>
        <option value="STRENGTH"   ${w.type==='STRENGTH'   ?'selected':''}>Strength</option>
        <option value="CARDIO"     ${w.type==='CARDIO'     ?'selected':''}>Cardio</option>
        <option value="FLEXIBILITY"${w.type==='FLEXIBILITY'?'selected':''}>Flexibility</option>
        <option value="HIIT"       ${w.type==='HIIT'       ?'selected':''}>HIIT</option>
        <option value="SPORT"      ${w.type==='SPORT'      ?'selected':''}>Sport</option>
        <option value="OTHER"      ${w.type==='OTHER'      ?'selected':''}>Other</option>
      </select>
      <input type="date" id="wk-date-${w.id}" value="${w.workoutDate || ''}"
        onchange="updateWorkoutField('${w.id}')"
        style="background:var(--bg);border:1px solid var(--border);border-radius:7px;padding:0.4rem 0.5rem;color:var(--text);font-size:0.82rem;outline:none"/>
    </div>
    ${exListHtml}
    <div class="ex-add-form" id="ex-add-form-${w.id}" style="display:none">
      <div class="form-row">
        <div class="form-group"><label>Exercise Name *</label><input type="text" id="ex-name-${w.id}" placeholder="e.g. Bench Press" onkeydown="if(event.key==='Enter'){event.preventDefault();saveExercise('${w.id}')}"/></div>
      </div>
      <div class="form-row">
        <div class="form-group"><label>Sets</label><input type="number" id="ex-sets-${w.id}" min="0" value="3"/></div>
        <div class="form-group"><label>Reps</label><input type="number" id="ex-reps-${w.id}" min="0" value="10"/></div>
        <div class="form-group"><label>Weight (kg)</label><input type="number" id="ex-wt-${w.id}" min="0" step="0.5" value="0"/></div>
      </div>
      <div class="form-group"><label>Notes</label><input type="text" id="ex-notes-${w.id}" placeholder="Optional notes…"/></div>
      <div style="display:flex;gap:0.5rem;margin-top:0.6rem">
        <button class="btn-secondary btn-sm" onclick="hideAddExerciseForm('${w.id}')">Cancel</button>
        <button class="btn-primary btn-sm" onclick="saveExercise('${w.id}')">Add Exercise</button>
      </div>
    </div>
    <button class="btn-secondary btn-sm" id="ex-add-btn-${w.id}" onclick="showAddExerciseForm('${w.id}')" style="margin-bottom:0.7rem">+ Add Exercise</button>
    <div class="wk-meta-row">
      <span>👥 ${hasCollab ? w.collaboratorIds.map(() => '<span class="wk-collab-avatar">C</span>').join('') : 'No collaborators'}</span>
      <button class="btn-icon" onclick="addWorkoutCollaborator('${w.id}')" style="font-size:0.75rem">+ Add Collab</button>
      ${w.durationMinutes ? `<span>⏱ ${w.durationMinutes}min</span>` : ''}
      ${w.caloriesBurned  ? `<span>🔥 ${w.caloriesBurned}kcal</span>` : ''}
    </div>
  </div>
</div>`;
}

function renderExerciseItem(wid, idx, ex) {
  const statusLower  = (ex.status || 'PENDING').toLowerCase();
  const statusLabels = { pending: '⭕ Pending', in_progress: '🔄 In Progress', done: '✅ Done' };
  return `
<div class="ex-item" id="ex-${wid}-${idx}">
  <div class="ex-item-header">
    <span class="ex-item-name">${esc(ex.name || 'Exercise ' + (idx + 1))}</span>
    <span class="ex-status ex-status-${statusLower}"
          onclick="cycleExerciseStatus('${wid}',${idx},'${ex.status||'PENDING'}')"
          title="Click to change status">${statusLabels[statusLower] || ex.status}</span>
  </div>
  <div class="ex-item-details">
    ${ex.sets      ? `<span>Sets: <strong>${ex.sets}</strong></span>`       : ''}
    ${ex.reps      ? `<span>Reps: <strong>${ex.reps}</strong></span>`       : ''}
    ${ex.weightKg  ? `<span>Weight: <strong>${ex.weightKg}kg</strong></span>` : ''}
    ${ex.durationSeconds ? `<span>Duration: <strong>${ex.durationSeconds}s</strong></span>` : ''}
    ${ex.notes     ? `<span style="color:var(--text-muted)">📝 ${esc(ex.notes)}</span>` : ''}
  </div>
  <div class="ex-item-actions">
    <button class="btn-icon" onclick="deleteExercise('${wid}',${idx})" title="Delete">🗑</button>
  </div>
</div>`;
}

function toggleWorkoutCard(id) {
  const body    = el(`wk-body-${id}`);
  if (!body) return;
  const isOpen = body.style.display !== 'none';
  body.style.display = isOpen ? 'none' : 'block';
}

/** Toggle the compact exercise preview panel (triggered by clicking the card header). */
function toggleExercisePreview(id) {
  const preview = el(`ex-preview-${id}`);
  if (!preview) return;
  preview.style.display = preview.style.display === 'none' ? 'block' : 'none';
}

// ==========================================================================
// CREATE WORKOUT (inline — no modal)
// ==========================================================================
function createNewWorkout() {
  if (el('wk-new-form')) { el('newWorkoutNameInput')?.focus(); return; }
  const listEl = el('activeWorkoutsList');
  if (!listEl) return;
  const card = document.createElement('div');
  card.className = 'wk-card wk-card-new';
  card.id = 'wk-new-form';
  card.innerHTML = `
    <div class="wk-name-edit-wrap">
      <input class="wk-name-input" id="newWorkoutNameInput"
             placeholder="Workout name (e.g. Leg Day)…" autofocus
             onkeydown="if(event.key==='Enter'){event.preventDefault();saveNewWorkout()}
                        else if(event.key==='Escape')cancelNewWorkout()"/>
      <button class="btn-primary  btn-sm" onclick="saveNewWorkout()">Create</button>
      <button class="btn-secondary btn-sm" onclick="cancelNewWorkout()">Cancel</button>
    </div>`;
  listEl.insertBefore(card, listEl.firstChild);
  el('newWorkoutNameInput')?.focus();
}

async function saveNewWorkout() {
  const name = (el('newWorkoutNameInput')?.value || '').trim();
  if (!name) { toast('Please enter a workout name', 'error'); return; }
  try {
    await apiPost('/api/workouts', { name, status: 'PENDING' });
    cancelNewWorkout();
    toast('Workout created! 💪', 'success');
    loadActiveWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

function cancelNewWorkout() { el('wk-new-form')?.remove(); }

// ==========================================================================
// WORKOUT UPDATES
// ==========================================================================
async function renameWorkout(id, name) {
  if (!name || !name.trim()) return;
  const w = _activeWorkouts.find(x => x.id === id);
  if (w && w.name === name.trim()) return;
  try {
    await apiPut(`/api/workouts/${id}`, { name: name.trim() });
    if (w) { w.name = name.trim(); }
    const nameEl = document.querySelector(`#wk-${id} .wk-card-name`);
    if (nameEl) nameEl.textContent = name.trim();
  } catch (err) { toast(err.message, 'error'); }
}

async function updateWorkoutField(id) {
  const w    = _activeWorkouts.find(x => x.id === id);
  const name = el(`wk-name-${id}`)?.value || (w ? w.name : '');
  const type = el(`wk-type-${id}`)?.value || undefined;
  const date = el(`wk-date-${id}`)?.value || undefined;
  const prevDate = w ? w.workoutDate : undefined;
  try {
    await apiPut(`/api/workouts/${id}`, { name, type, workoutDate: date });
    if (w) { w.type = type; w.workoutDate = date; }
    /* Re-render list when date changed so grouping stays accurate */
    if (date !== prevDate) await loadActiveWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

async function updateWorkoutStatus(id, status) {
  try {
    await apiPatch(`/api/workouts/${id}/status`, { status });
    const w      = _activeWorkouts.find(x => x.id === id);
    if (w) w.status = status;
    const badge  = document.querySelector(`#wk-${id} .wk-status`);
    if (badge) {
      badge.className = `wk-status wk-status-${status.toLowerCase()}`;
      const lbl = { DRAFT:'📝 Draft', PENDING:'⭕ Pending', IN_PROGRESS:'🔄 In Progress', COMPLETED:'✅ Completed' };
      badge.textContent = lbl[status] || status;
    }
    if (status === 'COMPLETED') {
      toast('🏆 Workout completed!', 'success');
      loadActiveWorkouts();
      if (_completedVisible) loadCompletedWorkouts();
    }
  } catch (err) { toast(err.message, 'error'); }
}

async function archiveWorkout(id) {
  try {
    const res = await apiPatch(`/api/workouts/${id}/archive`, {});
    toast(res.data?.archived ? '📦 Archived' : '📤 Unarchived');
    loadActiveWorkouts();
    if (_completedVisible) loadCompletedWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

async function deleteWorkout(id) {
  if (!confirm('Delete this workout? This cannot be undone.')) return;
  try {
    await apiDelete(`/api/workouts/${id}`);
    toast('Workout deleted');
    loadActiveWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

async function addWorkoutCollaborator(wid) {
  const email = prompt("Enter collaborator's email:");
  if (!email || !email.trim()) return;
  try {
    await apiPost(`/api/workouts/${wid}/collaborators`, { email: email.trim() });
    toast('Collaborator added! 👥', 'success');
    loadActiveWorkouts();
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// EXERCISES
// ==========================================================================
function showAddExerciseForm(wid) {
  const form = el(`ex-add-form-${wid}`);
  const btn  = el(`ex-add-btn-${wid}`);
  if (form) form.style.display = 'block';
  if (btn)  btn.style.display  = 'none';
  el(`ex-name-${wid}`)?.focus();
}

function hideAddExerciseForm(wid) {
  const form = el(`ex-add-form-${wid}`);
  const btn  = el(`ex-add-btn-${wid}`);
  if (form) form.style.display = 'none';
  if (btn)  btn.style.display  = '';
}

async function saveExercise(wid) {
  const name  = (el(`ex-name-${wid}`)?.value  || '').trim();
  if (!name) { toast('Exercise name required', 'error'); return; }
  const sets  = parseInt(el(`ex-sets-${wid}`)?.value)  || 0;
  const reps  = parseInt(el(`ex-reps-${wid}`)?.value)  || 0;
  const wt    = parseFloat(el(`ex-wt-${wid}`)?.value)  || 0;
  const notes = (el(`ex-notes-${wid}`)?.value || '').trim() || null;
  try {
    const res = await apiPost(`/api/workouts/${wid}/exercises`, { name, sets, reps, weightKg: wt, notes });
    const w   = res.data;
    const idx = _activeWorkouts.findIndex(x => x.id === wid);
    if (idx !== -1) _activeWorkouts[idx] = w;
    _replaceWorkoutCard(wid, w);
    toggleWorkoutCard(wid);
    toast('Exercise added! ✅', 'success');
    if (w.status === 'COMPLETED') {
      toast('🏆 All exercises done — Workout completed!', 'success');
      loadActiveWorkouts();
      if (_completedVisible) loadCompletedWorkouts();
    }
    const countEl = el('activeWorkoutsCount');
    if (countEl) countEl.textContent = _activeWorkouts.length;
  } catch (err) { toast(err.message, 'error'); }
}

async function cycleExerciseStatus(wid, idx, current) {
  const cycle  = { PENDING: 'IN_PROGRESS', IN_PROGRESS: 'DONE', DONE: 'PENDING' };
  const next   = cycle[current] || 'IN_PROGRESS';
  try {
    const res = await apiPatch(`/api/workouts/${wid}/exercises/${idx}/status`, { status: next });
    const w   = res.data;
    const wIdx = _activeWorkouts.findIndex(x => x.id === wid);
    if (wIdx !== -1) _activeWorkouts[wIdx] = w;

    // Update exercise badge in-place
    const exEl  = el(`ex-${wid}-${idx}`);
    if (exEl) {
      const badge  = exEl.querySelector('.ex-status');
      const labels = { PENDING: '⭕ Pending', IN_PROGRESS: '🔄 In Progress', DONE: '✅ Done' };
      if (badge) {
        badge.className   = `ex-status ex-status-${next.toLowerCase()}`;
        badge.textContent = labels[next] || next;
        badge.setAttribute('onclick', `cycleExerciseStatus('${wid}',${idx},'${next}')`);
      }
    }
    // Update workout status badge in-place
    const wkBadge = document.querySelector(`#wk-${wid} .wk-status`);
    if (wkBadge) {
      const sl = { DRAFT:'📝 Draft', PENDING:'⭕ Pending', IN_PROGRESS:'🔄 In Progress', COMPLETED:'✅ Completed' };
      wkBadge.className   = `wk-status wk-status-${w.status.toLowerCase()}`;
      wkBadge.textContent = sl[w.status] || w.status;
    }
    // Update progress counter in card header
    const exercises = w.exercises || [];
    const done      = exercises.filter(e => e.status === 'DONE').length;
    const doneTxt   = document.querySelector(`#wk-${wid} [data-progress]`);
    // (progress text update is minor; full re-render on completion)
    if (w.status === 'COMPLETED') {
      toast('🏆 All done — Workout completed!', 'success');
      loadActiveWorkouts();
      if (_completedVisible) loadCompletedWorkouts();
    }
  } catch (err) { toast(err.message, 'error'); }
}

async function deleteExercise(wid, idx) {
  if (!confirm('Delete this exercise?')) return;
  try {
    const res = await apiDelete(`/api/workouts/${wid}/exercises/${idx}`);
    const w   = res.data;
    const wIdx = _activeWorkouts.findIndex(x => x.id === wid);
    if (wIdx !== -1) _activeWorkouts[wIdx] = w;
    _replaceWorkoutCard(wid, w);
    toggleWorkoutCard(wid);
    toast('Exercise deleted');
  } catch (err) { toast(err.message, 'error'); }
}

/** Replace a workout card in the DOM with a freshly rendered one */
function _replaceWorkoutCard(id, w) {
  const oldCard = el(`wk-${id}`);
  if (!oldCard) return;
  const tmp = document.createElement('div');
  tmp.innerHTML = renderWorkoutCard(w);
  oldCard.replaceWith(tmp.firstElementChild);
}

// ==========================================================================
// FILES
// ==========================================================================

const uploadZone = document.getElementById('uploadZone');
const fileInput  = document.getElementById('fileInput');

if (uploadZone && fileInput) {
  uploadZone.addEventListener('click', () => fileInput.click());
  uploadZone.addEventListener('dragover',  e => { e.preventDefault(); uploadZone.classList.add('drag-over'); });
  uploadZone.addEventListener('dragleave', ()  => uploadZone.classList.remove('drag-over'));
  uploadZone.addEventListener('drop', e => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (file) uploadFile(file);
  });
  fileInput.addEventListener('change', e => {
    if (e.target.files[0]) uploadFile(e.target.files[0]);
  });
}

async function uploadFile(file) {
  toast('Uploading…');
  const form = new FormData();
  form.append('file', file);
  try {
    await apiUpload('/api/files', form);
    toast('File uploaded!');
    loadFiles();
  } catch (err) { toast(err.message, 'error'); }
}

async function loadFiles() {
  document.getElementById('fileList').innerHTML = '<div class="loading">Loading files…</div>';
  try {
    const res = await apiGet('/api/files');
    renderFiles(res.data || []);
  } catch (err) { toast(err.message, 'error'); }
}

function renderFiles(files) {
  const el = document.getElementById('fileList');
  if (!files.length) { el.innerHTML = '<p class="loading">No files uploaded yet.</p>'; return; }

  el.innerHTML = files.map(f => {
    const name        = esc(f.originalFilename);
    const ct          = (f.contentType || '').toLowerCase();
    const isImg       = ct.startsWith('image/');
    const isPdf       = ct === 'application/pdf';
    const downloadUrl = `/api/files/${f.id}/download`;
    const previewUrl  = `/api/files/${f.id}/preview`;

    let preview = '';
    if (isImg) {
      preview = `<div class="file-preview"><img src="${previewUrl}" alt="${name}" loading="lazy"/></div>`;
    } else if (isPdf) {
      preview = `<div class="file-preview"><iframe src="${previewUrl}" title="${name}"></iframe></div>`;
    }

    return `
      <div class="file-card">
        <div class="file-card-header">
          <span class="file-name">${isImg ? '🖼 ' : isPdf ? '📄 ' : '📎 '}${name}</span>
          <div style="display:flex;gap:0.4rem;flex-shrink:0;">
            <button class="btn-icon" title="Preview" onclick="window.open('${previewUrl}','_blank')">👁</button>
            <a href="${downloadUrl}" class="btn-icon" style="text-decoration:none" download="${name}" title="Download">⬇</a>
            <button class="btn-icon" onclick="deleteFile('${f.id}')" title="Delete">🗑</button>
          </div>
        </div>
        <div class="file-meta">${fileSize(f.fileSize)} · ${formatDate(f.uploadedAt)}</div>
        ${preview}
      </div>
    `;
  }).join('');
}

async function deleteFile(id) {
  if (!confirm('Delete this file?')) return;
  try {
    await apiDelete(`/api/files/${id}`);
    toast('File deleted');
    loadFiles();
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// PATCH helper — add to api.js if missing; graceful fallback handled in cycleStatus
// ==========================================================================
if (typeof apiPatch === 'undefined') {
  window.apiPatch = function(path, body) {
    return apiFetch(path, { method: 'PATCH', body: JSON.stringify(body) });
  };
}

// ==========================================================================
// PEOPLE / P2P CHAT COLLABORATION
// ==========================================================================

let currentPeerId   = null;
let currentPeerName = null;
let threadPollTimer = null;

async function searchUsers() {
  const q = (document.getElementById('userSearchInput').value || '').trim();
  if (!q) return;
  const resEl = document.getElementById('userSearchResults');
  resEl.innerHTML = '<div class="loading">Searching&hellip;</div>';
  resEl.classList.remove('hidden');
  try {
    const res = await apiGet(`/api/messages/users/search?q=${encodeURIComponent(q)}`);
    renderUserSearchResults(res.data || []);
  } catch (err) {
    resEl.innerHTML = `<div class="loading">Error: ${esc(err.message)}</div>`;
  }
}

function renderUserSearchResults(users) {
  const el = document.getElementById('userSearchResults');
  if (!users.length) { el.innerHTML = '<div class="user-result-item">No users found</div>'; return; }
  el.innerHTML = users.map(u => `
    <div class="user-result-item" onclick="openThread('${esc(u.id)}','${esc(u.name || u.email)}')">
      <div>
        <div class="user-result-name">${esc(u.name || u.email)}</div>
        <div class="user-result-email">${esc(u.email)}</div>
      </div>
      <button class="btn-primary btn-sm">Chat</button>
    </div>`).join('');
}

async function loadConversations() {
  const el = document.getElementById('conversationsList');
  el.innerHTML = '<div class="loading">Loading&hellip;</div>';
  try {
    const res = await apiGet('/api/messages/conversations');
    renderConversations(res.data || []);
  } catch (err) {
    el.innerHTML = `<div class="loading">Could not load conversations</div>`;
  }
}

function renderConversations(convs) {
  const el = document.getElementById('conversationsList');
  if (!convs.length) { el.innerHTML = '<p class="loading">No conversations yet. Search for a user above to start chatting.</p>'; return; }
  el.innerHTML = convs.map(c => {
    const initials = (c.peerName || '?').charAt(0).toUpperCase();
    const colorCode = c.peerId ? (c.peerId.charCodeAt(0) % 6) : 0;
    const avatarGrads = [
      'linear-gradient(135deg,#6c63ff,#a78bfa)',
      'linear-gradient(135deg,#10b981,#34d399)',
      'linear-gradient(135deg,#f59e0b,#fbbf24)',
      'linear-gradient(135deg,#ef4444,#f87171)',
      'linear-gradient(135deg,#3b82f6,#60a5fa)',
      'linear-gradient(135deg,#ec4899,#f9a8d4)'
    ];
    return `
    <div class="conversation-item" onclick="openThread('${esc(c.peerId)}','${esc(c.peerName)}')">
      <div class="convo-avatar" style="background:${avatarGrads[colorCode]}">${initials}</div>
      <div class="convo-info">
        <div class="conversation-peer">${esc(c.peerName)}</div>
        <div class="conversation-last">${esc(c.lastMessage || '')}</div>
      </div>
      ${c.unread > 0 ? `<span class="conversation-unread">${c.unread}</span>` : ''}
    </div>`;
  }).join('');
}

async function openThread(peerId, peerName) {
  currentPeerId   = peerId;
  currentPeerName = peerName;
  document.getElementById('chatThreadTitle').textContent = peerName;
  // Set avatar initials
  var avatar = document.getElementById('chatPeerAvatar');
  if (avatar) avatar.textContent = (peerName || '?').charAt(0).toUpperCase();
  // Hide all conversations-view elements
  ['peoplePageHeader','peopleSearchBar','userSearchResults','convSectionLabel','conversationsList'].forEach(function(id) {
    var el = document.getElementById(id); if (el) el.style.display = 'none';
  });
  // Hide FAB so it doesn't float over chat
  var fab = document.getElementById('forgeFabWrap');
  if (fab) fab.style.display = 'none';
  // Show thread
  document.getElementById('chatThread').classList.remove('hidden');
  await refreshThread();
  if (threadPollTimer) clearInterval(threadPollTimer);
  threadPollTimer = setInterval(refreshThread, 5000);
  // Scroll page to top (no scrollIntoView to avoid page jumping)
  var mainContent = document.querySelector('.main-content');
  if (mainContent) mainContent.scrollTop = 0;
  // Scroll messages to bottom
  var msgs = document.getElementById('threadMessages');
  if (msgs) msgs.scrollTop = msgs.scrollHeight;
}

async function refreshThread() {
  if (!currentPeerId) return;
  try {
    const res = await apiGet(`/api/messages/conversation/${currentPeerId}`);
    const myId = localStorage.getItem('userId') || '';
    renderThreadMessages(res.data || [], myId);
    refreshUnreadBadge();
  } catch (e) { /* silent */ }
}

function renderThreadMessages(msgs, myId) {
  const el = document.getElementById('threadMessages');
  const wasAtBottom = el.scrollHeight - el.scrollTop <= el.clientHeight + 60;
  el.innerHTML = msgs.map(m => {
    const mine = m.senderId === myId;
    const t    = m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) : '';
    return `<div class="chat-bubble ${mine ? 'mine' : 'theirs'}">
      ${esc(m.content)}
      <div class="bubble-time">${t}</div>
    </div>`;
  }).join('');
  if (wasAtBottom || msgs.length < 5) el.scrollTop = el.scrollHeight;
}

function closeThread() {
  if (threadPollTimer) { clearInterval(threadPollTimer); threadPollTimer = null; }
  currentPeerId   = null;
  currentPeerName = null;
  document.getElementById('chatThread').classList.add('hidden');
  // Restore conversations-view elements
  ['peoplePageHeader','peopleSearchBar','convSectionLabel','conversationsList'].forEach(function(id) {
    var el = document.getElementById(id); if (el) el.style.display = '';
  });
  // Restore FAB
  var fab = document.getElementById('forgeFabWrap');
  if (fab) fab.style.display = '';
  loadConversations();
}

async function sendThreadMessage() {
  const inp = document.getElementById('threadInput');
  const content = (inp.value || '').trim();
  if (!content || !currentPeerId) return;
  inp.value = '';
  try {
    await apiPost('/api/messages', { recipientId: currentPeerId, content });
    await refreshThread();
  } catch (err) { toast(err.message, 'error'); }
}

// Allow Enter (without Shift) to send in thread
document.getElementById('threadInput').addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendThreadMessage(); }
});

async function refreshUnreadBadge() {
  try {
    const res = await apiGet('/api/messages/unread-count');
    const count = res.data || 0;
    const badge = document.getElementById('peopleBadge');
    if (badge) {
      badge.textContent = count > 9 ? '9+' : String(count);
      if (count > 0) badge.classList.remove('hidden');
      else badge.classList.add('hidden');
    }
  } catch (e) { /* silent */ }
}

// Poll unread badge every 15s
setInterval(refreshUnreadBadge, 15000);
refreshUnreadBadge();

// Load conversations when People tab is activated
document.querySelectorAll('.nav-item[data-tab="people"]').forEach(el => {
  el.addEventListener('click', () => {
    loadConversations();
    refreshUnreadBadge();
  });
});

// ==========================================================================
// Initial load
// ==========================================================================
loadTasks();
loadRecurringTasks();
loadStreaks();

// ==========================================================================
// INNER SUB-TAB SWITCHING (delegates for Fitness & Collab tab bars)
// ==========================================================================
document.addEventListener('click', e => {
  const btn = e.target.closest('.inner-tab');
  if (!btn) return;
  const itab = btn.dataset.itab;
  if (!itab) return;
  // Find sibling tabs and contents in the same parent
  const bar = btn.closest('.inner-tab-bar');
  if (!bar) return;
  bar.querySelectorAll('.inner-tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  // Find content containers in the same section parent
  const section = bar.closest('section') || bar.parentElement;
  section.querySelectorAll('.inner-tab-content').forEach(c => {
    c.classList.remove('active');
    c.style.display = 'none';
  });
  const target = document.getElementById(`itab-${itab}`);
  if (target) {
    target.classList.add('active');
    target.style.display = 'block';
  }
  // Lazy loads
  if (itab === 'fitMain')         { initFitnessTab(); }
  if (itab === 'fitCategories')   loadFitnessCategories();
  if (itab === 'fitCalendar')     { hideFitFab(); initFitCalendar(); }
  if (itab === 'sessionTracker')  { hideFitFab(); loadSessions(); }
  if (itab === 'photoGallery')    { hideFitFab(); loadProgressPhotos(); }
  if (itab === 'brainChallenges') loadChallenges();
  if (itab === 'brainDecisions')  loadDecisionLogs();
  if (itab === 'collabTasks')   loadCollabTasks();
  if (itab === 'collabGoals')    loadCollabGoals();
  if (itab === 'collabFiles')    loadCollabFiles();
  if (itab === 'collabWorkouts') loadCollabWorkouts();
});

// ==========================================================================
// COLLABORATION SECTION
// ==========================================================================
async function loadCollabSection() {
  loadCollabTasks();
  loadCollabGoals();
  loadCollabFiles();
  loadCollabWorkouts();
}

// Cache for resolved user names: { userId → "Name" }
const _userNameCache = {};
async function resolveUserName(uid) {
  if (!uid) return 'Unknown';
  if (_userNameCache[uid]) return _userNameCache[uid];
  try {
    const r = await apiGet(`/api/users/${uid}/public`);
    const name = (r.data && r.data.name) ? r.data.name : (r.data && r.data.email ? r.data.email : uid.slice(-6));
    _userNameCache[uid] = name;
    return name;
  } catch (_) { return uid.slice(-6); }
}

async function loadCollabTasks() {
  const listEl = document.getElementById('collabTaskList');
  if (!listEl) return;
  listEl.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res  = await apiGet('/api/tasks');
    const myId = localStorage.getItem('userId') || '';
    const allT = res.data || [];

    // Tasks shared WITH me (I am a collaborator, not the owner)
    const sharedTasks = allT.filter(t =>
      t.collaboratorIds && t.collaboratorIds.includes(myId) && t.userId !== myId
    );

    // My tasks that have collaborators (for progress overview)
    const myOwnedWithCollabs = allT.filter(t =>
      t.userId === myId && t.collaboratorIds && t.collaboratorIds.length > 0
    );

    if (!sharedTasks.length && !myOwnedWithCollabs.length) {
      listEl.innerHTML = '<p class="loading">No collaborative tasks yet.</p>';
      return;
    }

    // Collect all unique user IDs that need name resolution
    const idsToResolve = new Set();
    sharedTasks.forEach(t => {
      idsToResolve.add(t.userId);
      if (t.collaboratorProgress) Object.keys(t.collaboratorProgress).forEach(id => idsToResolve.add(id));
    });
    myOwnedWithCollabs.forEach(t => {
      t.collaboratorIds.forEach(id => idsToResolve.add(id));
      if (t.collaboratorProgress) Object.keys(t.collaboratorProgress).forEach(id => idsToResolve.add(id));
    });
    // Resolve all names in parallel
    await Promise.all([...idsToResolve].map(id => resolveUserName(id)));

    const statusLabels = { PENDING: '⭕ Pending', IN_PROGRESS: '🔄 In Progress', DONE: '✅ Done', COMPLETED: '✅ Done', CANCELLED: '🚫 Cancelled' };
    const name = id => id === myId ? 'You' : (_userNameCache[id] || id.slice(-6));

    let html = '';

    // ── Tasks shared WITH me ─────────────────────────────────────
    if (sharedTasks.length) {
      html += `<div style="font-weight:600;font-size:0.82rem;color:var(--text-muted);margin-bottom:0.4rem;padding:0 0.25rem">📋 SHARED WITH ME</div>`;
      html += sharedTasks.map(t => {
        const ownerName = name(t.userId);
        const myProg  = (t.collaboratorProgress && t.collaboratorProgress[myId]) || null;
        const pct     = myProg ? myProg.completionPct : null;
        const note    = myProg ? (myProg.note || '') : '';
        const progBar = pct != null
          ? `<div style="height:4px;border-radius:2px;background:var(--border);margin:0.3rem 0.5rem">
               <div style="height:100%;border-radius:2px;background:#7c3aed;width:${pct}%"></div>
             </div>`
          : '';
        const safeNote = (note || '').replace(/'/g, "&#39;");
        // Other collaborators on this task
        const otherCollabs = (t.collaboratorIds || []).filter(id => id !== myId);
        const collabNames = otherCollabs.map(id => name(id)).join(', ');
        return `
        <div class="task-card" style="margin-bottom:0.65rem" data-collab-task="${t.id}">
          <div class="task-card-header">
            <span class="task-title">${esc(t.title)}</span>
            <button class="btn-status status-${(t.status||'pending').toLowerCase()}"
                    onclick="collabCycleTaskStatus('${t.id}','${t.status||'PENDING'}')"
                    title="Click to cycle status">
              ${statusLabels[t.status] || t.status}
            </button>
          </div>
          ${progBar}
          <div class="task-meta">
            <span>👤 Owner: <strong>${esc(ownerName)}</strong></span>
            ${collabNames ? `<span>👥 Also: ${esc(collabNames)}</span>` : ''}
            ${t.priority ? `<span>${esc(t.priority)}</span>` : ''}
            ${t.dueDate  ? `<span>Due: ${t.dueDate.slice(0,10)}</span>` : ''}
            ${pct != null ? `<span style="color:#7c3aed;font-weight:600">My progress: ${pct}%</span>` : '<span style="color:var(--text-muted)">No progress logged</span>'}
            ${note ? `<span title="${esc(note)}">📝 ${esc(note.length > 30 ? note.slice(0,30)+'…' : note)}</span>` : ''}
          </div>
          <div style="padding:0.25rem 0.5rem 0.35rem;display:flex;gap:0.4rem;flex-wrap:wrap">
            <button class="btn-primary btn-xs" onclick="openProgressModal('${t.id}',${pct != null ? pct : 0},'${safeNote}')">📊 My Progress</button>
            <button class="btn-secondary btn-xs" onclick="openCollabDiscuss('${t.id}','${t.userId}')">💬 Discuss</button>
            <button class="btn-secondary btn-xs" style="color:#ef4444;border-color:#ef4444" onclick="leaveCollab('${t.id}')">🚪 Leave</button>
          </div>
        </div>`;
      }).join('');
    }

    // ── My tasks with collaborators: show their progress ─────────
    if (myOwnedWithCollabs.length) {
      html += `<div style="font-weight:600;font-size:0.82rem;color:var(--text-muted);margin:0.8rem 0 0.4rem;padding:0 0.25rem">👥 MY TASKS — COLLABORATOR PROGRESS</div>`;
      html += myOwnedWithCollabs.map(t => {
        const collabNames = t.collaboratorIds.map(id => name(id)).join(', ');
        const progEntries = t.collaboratorProgress ? Object.entries(t.collaboratorProgress) : [];
        const progSummary = progEntries.length
          ? progEntries.map(([uid, p]) =>
              `<div style="display:flex;align-items:center;gap:0.4rem;padding:0.25rem 0;border-bottom:1px solid var(--border)">
                 <span style="font-weight:600;color:var(--text);font-size:0.82rem">👤 ${esc(name(uid))}</span>
                 <span style="font-size:0.78rem;color:#7c3aed;font-weight:600">${p.completionPct}%</span>
                 <div style="flex:1;height:5px;border-radius:3px;background:var(--border);overflow:hidden">
                   <div style="height:100%;border-radius:3px;background:#7c3aed;width:${p.completionPct}%"></div>
                 </div>
                 ${p.note ? `<span style="font-size:0.75rem;color:var(--text-muted)" title="${esc(p.note)}">📝 ${esc(p.note.length>20?p.note.slice(0,20)+'…':p.note)}</span>` : ''}
               </div>`
            ).join('')
          : '<span style="color:var(--text-muted);font-size:0.78rem">No progress logged yet</span>';
        return `
        <div class="task-card" style="margin-bottom:0.65rem;border-left:3px solid #059669">
          <div class="task-card-header">
            <span class="task-title">${esc(t.title)}</span>
            <span class="task-status status-${(t.status||'pending').toLowerCase()}">${statusLabels[t.status] || t.status}</span>
          </div>
          <div style="padding:0.25rem 0.5rem;font-size:0.78rem;color:var(--text-muted)">👥 Collaborators: <strong style="color:var(--text)">${esc(collabNames)}</strong></div>
          <div style="padding:0.1rem 0.5rem 0.4rem">
            ${progSummary}
          </div>
        </div>`;
      }).join('');
    }

    listEl.innerHTML = html;
  } catch (err) { listEl.innerHTML = `<p class="loading">${esc(err.message)}</p>`; }
}

async function collabCycleTaskStatus(id, current) {
  const cycle = { PENDING: 'IN_PROGRESS', IN_PROGRESS: 'DONE', DONE: 'PENDING', COMPLETED: 'PENDING' };
  const next  = cycle[current] || 'IN_PROGRESS';
  try {
    await apiPatch(`/api/tasks/${id}/status`, { status: next });
    loadCollabTasks(); // refresh the list
  } catch (err) { toast(err.message || 'Failed to update status', 'error'); }
}

/**
 * Switch to the People tab and open a chat thread with the task owner.
 * @param {string} taskId - not used directly, kept for future reference
 * @param {string} ownerId - the userId of the task owner
 */
async function openCollabDiscuss(taskId, ownerId) {
  // Navigate to People tab
  document.querySelectorAll('.nav-item').forEach(l => l.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  const peopleNav = document.querySelector('.nav-item[data-tab="people"]');
  if (peopleNav) peopleNav.classList.add('active');
  const peopleTab = document.getElementById('tab-people');
  if (peopleTab) peopleTab.classList.add('active');
  // Resolve the owner's name then open thread
  try {
    const r = await apiGet(`/api/users/${ownerId}/public`);
    const name = (r.data && r.data.name) ? r.data.name : (r.data && r.data.email ? r.data.email : 'Task Owner');
    openThread(ownerId, name);
  } catch (_) {
    openThread(ownerId, 'Task Owner');
  }
}

/**
 * Remove the current user from a shared task's collaborator list.
 */
async function leaveCollab(taskId) {
  if (!confirm('Leave this shared task? You will no longer see it in your Collab list.')) return;
  try {
    await apiDelete(`/api/tasks/${taskId}/leave`);
    toast('You have left the task', 'success');
    loadCollabTasks();
  } catch (err) { toast(err.message || 'Failed to leave task', 'error'); }
}


async function loadCollabGoals() {
  const el = document.getElementById('collabGoalList');
  if (!el) return;
  el.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res = await apiGet('/api/goals');
    const myId = localStorage.getItem('userId') || '';
    // Show goals where the current user is a COLLABORATOR (not the owner)
    const goals = (res.data || []).filter(g =>
      g.collaboratorIds && g.collaboratorIds.includes(myId) && g.userId !== myId
    );
    if (!goals.length) { el.innerHTML = '<p class="loading">No goals shared with you yet.</p>'; return; }
    el.innerHTML = goals.map(g => `
      <div class="goal-card">
        <div class="goal-card-header">
          <span class="goal-title">${esc(g.title)}</span>
          <span class="goal-category">${esc(g.category||'')}</span>
        </div>
        <div class="task-meta">
          <span>🎯 Shared with you</span>
          <span>Progress: ${g.progressPercent||0}%</span>
          ${g.targetDate ? `<span>Target: ${g.targetDate.slice(0,10)}</span>` : ''}
        </div>
      </div>`).join('');
  } catch (err) { el.innerHTML = `<p class="loading">${esc(err.message)}</p>`; }
}

async function loadCollabWorkouts() {
  const listEl = document.getElementById('collabWorkoutList');
  if (!listEl) return;
  listEl.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res = await apiGet('/api/workouts/all');
    // Show workouts where the current user is a collaborator (not the owner)
    const userId = (localStorage.getItem('userId') || '').toLowerCase();
    const shared = (res.data || []).filter(w =>
      w.collaboratorIds && w.collaboratorIds.some(cid => cid.toLowerCase() === userId)
    );
    if (!shared.length) {
      listEl.innerHTML = '<p class="loading">No workouts shared with you yet.</p>';
      return;
    }
    listEl.innerHTML = shared.map(w => {
      const statusLower = (w.status || 'draft').toLowerCase();
      const statusLabels = { draft: '📝 Draft', pending: '⭕ Pending', in_progress: '🔄 In Progress', completed: '✅ Completed', archived: '📦 Archived' };
      const exercises = w.exercises || [];
      const done = exercises.filter(e => e.status === 'DONE').length;
      return `
        <div class="wk-card" style="border-left:3px solid #6c63ff">
          <div class="wk-card-header">
            <div class="wk-card-left">
              <span class="wk-card-name">${esc(w.name)}</span>
              <div style="display:flex;gap:0.4rem;flex-wrap:wrap;margin-top:0.1rem">
                <span class="wk-status wk-status-${statusLower}">${statusLabels[statusLower] || w.status}</span>
                ${w.type ? `<span style="font-size:0.7rem;color:var(--text-muted)">${w.type}</span>` : ''}
                ${exercises.length ? `<span style="font-size:0.7rem;color:var(--text-muted)">${done}/${exercises.length} done</span>` : ''}
              </div>
            </div>
          </div>
          ${exercises.length ? `
            <div class="ex-list" style="padding:0.5rem 0.75rem">
              ${exercises.map((ex, idx) => `
                <div class="ex-item">
                  <div class="ex-item-header">
                    <span class="ex-item-name">${esc(ex.name || 'Exercise ' + (idx+1))}</span>
                    <span class="ex-status ex-status-${(ex.status||'PENDING').toLowerCase()}"
                          onclick="cycleExerciseStatus('${w.id}',${idx},'${ex.status||'PENDING'}')"
                          title="Click to update status">${{PENDING:'⭕ Pending', IN_PROGRESS:'🔄 In Progress', DONE:'✅ Done'}[ex.status] || ex.status}</span>
                  </div>
                </div>`).join('')}
            </div>` : ''}
        </div>`;
    }).join('');
  } catch (err) {
    if (listEl) listEl.innerHTML = `<p class="loading">${esc(err.message)}</p>`;
  }
}

async function loadCollabFiles() {
  const el = document.getElementById('collabFileList');
  if (!el) return;
  el.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res = await apiGet('/api/files');
    const files = (res.data || []).filter(f => f.collaboratorIds && f.collaboratorIds.length > 0);
    if (!files.length) { el.innerHTML = '<p class="loading">No shared files yet.</p>'; return; }
    el.innerHTML = files.map(f => {
      const name = esc(f.originalFilename);
      const ct = (f.contentType||'').toLowerCase();
      const isImg = ct.startsWith('image/');
      const isPdf = ct === 'application/pdf';
      const icon  = isImg ? '🖼' : isPdf ? '📄' : '📎';
      return `<div class="file-card">
        <div class="file-card-header">
          <span class="file-name">${icon} ${name}</span>
          <div style="display:flex;gap:0.4rem">
            <a href="/api/files/${f.id}/preview" target="_blank" class="btn-icon" title="Preview">👁</a>
            <a href="/api/files/${f.id}/download" download="${name}" class="btn-icon" title="Download">⬇</a>
          </div>
        </div>
        <div class="file-meta">👥 ${f.collaboratorIds.length} collaborator(s) · ${fileSize(f.fileSize)}</div>
      </div>`;
    }).join('');
  } catch (err) { el.innerHTML = `<p class="loading">${esc(err.message)}</p>`; }
}

// ==========================================================================
// FITNESS CATEGORIES MANAGEMENT
// ==========================================================================
async function loadFitnessCategories() {
  const el = document.getElementById('fitCategoryList');
  if (!el) return;
  el.innerHTML = '<div class="loading">Loading categories…</div>';
  try {
    const res = await apiGet('/api/fitness/categories');
    const cats = res.data || [];
    if (!cats.length) { el.innerHTML = '<p class="loading">No categories yet.</p>'; return; }
    el.innerHTML = cats.map(c => `
      <div class="cat-item">
        <div style="display:flex;align-items:center;gap:0.5rem;flex:1">
          <span class="cat-item-name">${esc(c.name)}</span>
          ${c.userId === null ? '<span class="cat-item-tag">System</span>' : '<span class="cat-item-tag" style="color:#6c63ff">Custom</span>'}
        </div>
        ${c.editable ? `<button class="btn-icon" onclick="deleteFitnessCategory('${c.id}')" title="Delete">🗑</button>` : ''}
      </div>`).join('');
  } catch (err) { el.innerHTML = `<p class="loading">${esc(err.message)}</p>`; }
}

async function promptAddFitnessCategory() {
  const name = prompt('Enter new category name (e.g. YOGA, SWIMMING, PILATES):');
  if (!name || !name.trim()) return;
  try {
    await apiPost('/api/fitness/categories', { name: name.trim().toUpperCase() });
    toast('Category added!');
    loadFitnessCategories();
  } catch (err) { toast(err.message, 'error'); }
}

async function deleteFitnessCategory(id) {
  if (!confirm('Delete this category?')) return;
  try {
    await apiDelete(`/api/fitness/categories/${id}`);
    toast('Category deleted');
    loadFitnessCategories();
  } catch (err) { toast(err.message, 'error'); }
}

// ==========================================================================
// CALENDAR ENGINE  (shared between Universal + Fitness mini-calendar)
// ==========================================================================

// State
let _uniCalYear = new Date().getFullYear();
let _uniCalMonth = new Date().getMonth(); // 0-indexed
let _fitCalYear = new Date().getFullYear();
let _fitCalMonth = new Date().getMonth();

const MONTH_NAMES = ['January','February','March','April','May','June',
                     'July','August','September','October','November','December'];

/**
 * Build a calendar grid for the given month/year.
 * taskDates = Set of "YYYY-MM-DD" strings that have tasks.
 */
function buildCalendarGrid(year, month, taskDates, gridId, labelId, onClickDate) {
  const labelEl = document.getElementById(labelId);
  const gridEl  = document.getElementById(gridId);
  if (!labelEl || !gridEl) return;

  labelEl.textContent = `${MONTH_NAMES[month]} ${year}`;

  const today     = new Date();
  const todayStr  = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;
  const firstDay  = new Date(year, month, 1).getDay(); // 0=Sun
  const daysInMon = new Date(year, month+1, 0).getDate();

  let html = '';
  // Empty leading cells
  for (let i = 0; i < firstDay; i++) html += '<div class="cal-cell empty"></div>';
  // Day cells
  for (let d = 1; d <= daysInMon; d++) {
    const dateStr = `${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
    const isToday = dateStr === todayStr;
    const hasTasks = taskDates.has(dateStr);
    const cls = ['cal-cell', isToday ? 'today' : '', hasTasks ? 'has-tasks' : ''].filter(Boolean).join(' ');
    const click = hasTasks ? `onclick="${onClickDate}('${dateStr}')"` : '';
    html += `<div class="${cls}" ${click}>${d}</div>`;
  }
  gridEl.innerHTML = html;
}

/** Fetch all tasks and return a Set of "YYYY-MM-DD" strings that have tasks */
async function fetchTaskDatesSet() {
  try {
    const res  = await apiGet('/api/tasks');
    const tasks = res.data || [];
    const dates = new Set();
    tasks.forEach(t => {
      if (t.scheduledDate) dates.add(t.scheduledDate.slice(0,10));
      if (t.dueDate)       dates.add(t.dueDate.slice(0,10));
    });
    return { dates, tasks };
  } catch (_) { return { dates: new Set(), tasks: [] }; }
}

// ---- UNIVERSAL CALENDAR ----
async function openCalendarOverlay() {
  document.getElementById('calendarOverlay').classList.remove('hidden');
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_uniCalYear, _uniCalMonth, dates, 'uniCalGrid', 'uniCalMonthLabel', 'openDayPopup');
}
function closeCalendarOverlay() {
  document.getElementById('calendarOverlay').classList.add('hidden');
}
async function uniCalPrev() {
  _uniCalMonth--;
  if (_uniCalMonth < 0) { _uniCalMonth = 11; _uniCalYear--; }
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_uniCalYear, _uniCalMonth, dates, 'uniCalGrid', 'uniCalMonthLabel', 'openDayPopup');
}
async function uniCalNext() {
  _uniCalMonth++;
  if (_uniCalMonth > 11) { _uniCalMonth = 0; _uniCalYear++; }
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_uniCalYear, _uniCalMonth, dates, 'uniCalGrid', 'uniCalMonthLabel', 'openDayPopup');
}

// ---- FITNESS MINI CALENDAR ----
async function initFitCalendar() {
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_fitCalYear, _fitCalMonth, dates, 'fitCalGrid', 'fitCalMonthLabel', 'openDayPopup');
}
async function fitCalPrev() {
  _fitCalMonth--;
  if (_fitCalMonth < 0) { _fitCalMonth = 11; _fitCalYear--; }
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_fitCalYear, _fitCalMonth, dates, 'fitCalGrid', 'fitCalMonthLabel', 'openDayPopup');
}
async function fitCalNext() {
  _fitCalMonth++;
  if (_fitCalMonth > 11) { _fitCalMonth = 0; _fitCalYear++; }
  const { dates } = await fetchTaskDatesSet();
  buildCalendarGrid(_fitCalYear, _fitCalMonth, dates, 'fitCalGrid', 'fitCalMonthLabel', 'openDayPopup');
}

// ---- DAY DETAIL POPUP ----
async function openDayPopup(dateStr) {
  const popup   = document.getElementById('calDayPopup');
  const titleEl = document.getElementById('calDayPopupTitle');
  const bodyEl  = document.getElementById('calDayPopupBody');
  if (!popup) return;

  // Format nice date
  const d = new Date(dateStr + 'T00:00:00');
  titleEl.textContent = d.toLocaleDateString(undefined, { weekday:'long', year:'numeric', month:'long', day:'numeric' });
  bodyEl.innerHTML = '<div class="loading">Loading tasks…</div>';
  popup.classList.remove('hidden');

  try {
    const res   = await apiGet('/api/tasks');
    const tasks = (res.data || []).filter(t => {
      const sd = t.scheduledDate ? t.scheduledDate.slice(0,10) : null;
      const dd = t.dueDate       ? t.dueDate.slice(0,10)       : null;
      return sd === dateStr || dd === dateStr;
    });

    if (!tasks.length) {
      bodyEl.innerHTML = '<p style="color:var(--text-secondary);font-size:0.9rem;text-align:center;padding:1rem 0">No tasks on this day.</p>';
      return;
    }

    bodyEl.innerHTML = tasks.map(t => {
      const status = (t.status||'PENDING').toUpperCase();
      const dotCls = status === 'DONE' || status === 'COMPLETED' ? 'done'
                   : status === 'IN_PROGRESS' ? 'inprog' : '';
      return `
        <div class="cal-popup-task">
          <div class="cal-popup-task-dot ${dotCls}"></div>
          <div class="cal-popup-task-info">
            <div class="cal-popup-task-title">${esc(t.title)}</div>
            <div class="cal-popup-task-status">${status.replace('_',' ')} · ${t.priority||'MEDIUM'}</div>
          </div>
        </div>`;
    }).join('');
  } catch (err) {
    bodyEl.innerHTML = `<p style="color:#ef4444;font-size:0.9rem">${esc(err.message)}</p>`;
  }
}

function closeDayPopup() {
  document.getElementById('calDayPopup').classList.add('hidden');
}

// Close calendar/popup on backdrop click
document.getElementById('calendarOverlay').addEventListener('click', e => {
  if (e.target === document.getElementById('calendarOverlay')) closeCalendarOverlay();
});
document.getElementById('calDayPopup').addEventListener('click', e => {
  if (e.target === document.getElementById('calDayPopup')) closeDayPopup();
});

// ==========================================================================
// DAILY NOTES  (card-grid redesign)
// ==========================================================================

let _currentNoteColor = 'default';
let _noteViewId       = null;
let _noteViewPinned   = false;

/** Format a Date to YYYY-MM-DD using LOCAL timezone */
function _localDateStr(d) {
  return d.getFullYear() + '-'
    + String(d.getMonth() + 1).padStart(2, '0') + '-'
    + String(d.getDate()).padStart(2, '0');
}

function initNotesTab() {
  loadNoteCards();
}

// ---------- Create card -------------------------------------------------------
function openNewNote() {
  el('noteCreateTitle').value   = '';
  el('noteCreateContent').value = '';
  _currentNoteColor = 'default';
  document.querySelectorAll('.note-color-btn').forEach(b => b.classList.remove('selected'));
  el('noteCreateModal').classList.remove('hidden');
  setTimeout(() => el('noteCreateTitle').focus(), 80);
}

function closeNewNote() {
  el('noteCreateModal').classList.add('hidden');
}

function setNoteColor(color) {
  _currentNoteColor = color;
  document.querySelectorAll('.note-color-btn').forEach(b => {
    b.classList.toggle('selected', b.dataset.color === color);
  });
}

async function saveNewNote() {
  const title   = el('noteCreateTitle').value.trim();
  const content = el('noteCreateContent').value.trim();
  if (!title && !content) { toast('Write something first', 'error'); return; }
  const today   = _localDateStr(new Date());
  try {
    await apiPost('/api/notes/new', { date: today, title: title || null, content, color: _currentNoteColor });
    closeNewNote();
    toast('Note saved ✅', 'success');
    loadNoteCards();
  } catch (err) { toast(err.message, 'error'); }
}

// ---------- Card grid ---------------------------------------------------------
async function loadNoteCards() {
  const grid = el('noteCardsGrid');
  const hd   = el('notesSectionHd');
  if (grid) grid.innerHTML = '<div class="loading">Loading notes…</div>';
  try {
    const kw  = el('noteSearchInput')?.value?.trim();
    let url   = '/api/notes/search';
    if (kw)   url += `?keyword=${encodeURIComponent(kw)}`;
    const res  = await apiGet(url);
    const notes = (res.data || []).sort((a, b) => {
      if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
      const aTs = a.updatedAt || a.date || '';
      const bTs = b.updatedAt || b.date || '';
      return bTs.localeCompare(aTs);
    });
    if (hd) hd.style.display = notes.length ? 'block' : 'none';
    if (!notes.length) {
      grid.innerHTML = '<p class="notes-empty">No notes yet. Hit <strong>+ New Note</strong> to start.</p>';
      return;
    }
    grid.innerHTML = notes.map(renderNoteCard).join('');
  } catch (err) {
    if (grid) grid.innerHTML = `<p class="notes-empty">${esc(err.message)}</p>`;
  }
}

function renderNoteCard(n) {
  const title   = n.title ? esc(n.title) : esc(_fmtNoteDate(n.date));
  const snippet = esc((n.content || '').slice(0, 140));
  const ellipsis = (n.content || '').length > 140 ? '…' : '';
  const bg      = n.color && n.color !== 'default' ? n.color : '';
  const bgStyle = bg ? `style="background:${bg}"` : '';
  const ts      = n.updatedAt ? _fmtNoteTs(n.updatedAt) : (n.date || '');
  return `<div class="note-card${n.pinned ? ' note-pinned' : ''}" ${bgStyle} onclick="openNoteView('${n.id}')">
    ${n.pinned ? '<span class="note-pin-icon">📌</span>' : ''}
    <div class="note-card-title">${title}</div>
    ${snippet ? `<div class="note-card-snippet">${snippet}${ellipsis}</div>` : ''}
    <div class="note-card-ts">${esc(ts)}</div>
  </div>`;
}

function _fmtNoteDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function _fmtNoteTs(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  if (isNaN(d)) return ts;
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short' }) + ', ' +
         d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

// Search (inline, fires on input)
function searchNotes() { loadNoteCards(); }

// ---------- Note View Modal ---------------------------------------------------
async function openNoteView(id) {
  _noteViewId = id;
  try {
    const res  = await apiGet(`/api/notes/${id}`);
    const n    = res.data;
    if (!n) return;
    _noteViewPinned = n.pinned;
    el('noteViewTitle').textContent  = n.title || _fmtNoteDate(n.date);
    el('noteViewContent').innerHTML  = n.content
      ? n.content.replace(/\n/g, '<br>') : '<em style="opacity:.5">Empty note</em>';
    el('noteViewUpdated').textContent = n.updatedAt
      ? 'Last updated ' + _fmtNoteTs(n.updatedAt) : '';
    el('notePinBtn').textContent = n.pinned ? '📌 Unpin' : '📌 Pin';
    // color the modal header if note has a color
    const mc = document.querySelector('#noteViewModal .note-view-header');
    if (mc) mc.style.background = (n.color && n.color !== 'default') ? n.color : '';
    el('noteViewModal').classList.remove('hidden');
  } catch (err) { toast(err.message, 'error'); }
}

function closeNoteView() {
  el('noteViewModal').classList.add('hidden');
  _noteViewId = null;
}

async function toggleNotePin() {
  if (!_noteViewId) return;
  try {
    await apiPatch(`/api/notes/${_noteViewId}`, { pinned: !_noteViewPinned });
    closeNoteView();
    loadNoteCards();
    toast(_noteViewPinned ? 'Note unpinned' : 'Note pinned 📌', 'success');
  } catch (err) { toast(err.message, 'error'); }
}

async function deleteNoteFromView() {
  if (!_noteViewId) return;
  if (!confirm('Delete this note? This cannot be undone.')) return;
  try {
    await apiDelete(`/api/notes/${_noteViewId}`);
    closeNoteView();
    loadNoteCards();
    toast('Note deleted', 'success');
  } catch (err) { toast(err.message, 'error'); }
}

function editNoteFromView() {
  if (!_noteViewId) return;
  const titleEl   = el('noteViewTitle');
  const contentEl = el('noteViewContent');
  // Make them editable inline
  titleEl.contentEditable   = 'true';
  contentEl.contentEditable = 'true';
  titleEl.style.outline     = '1px solid var(--accent)';
  contentEl.style.outline   = '1px solid var(--accent)';
  titleEl.focus();
  el('notePinBtn').textContent = '💾 Save';
  el('notePinBtn').onclick     = saveNoteFromView;
}

async function saveNoteFromView() {
  if (!_noteViewId) return;
  const title   = el('noteViewTitle').textContent.trim();
  const content = el('noteViewContent').innerText.trim();
  try {
    await apiPatch(`/api/notes/${_noteViewId}`, { title, content });
    // Reset editable state
    el('noteViewTitle').contentEditable   = 'false';
    el('noteViewContent').contentEditable = 'false';
    el('noteViewTitle').style.outline     = '';
    el('noteViewContent').style.outline   = '';
    el('notePinBtn').textContent = _noteViewPinned ? '📌 Unpin' : '📌 Pin';
    el('notePinBtn').onclick     = toggleNotePin;
    toast('Note saved ✅', 'success');
    loadNoteCards();
  } catch (err) { toast(err.message, 'error'); }
}

// Backwards-compat stubs (some event-listeners or other code might reference these)
function deleteNote()    { deleteNoteFromView(); }
function setMood()       {}
function onNoteInput()   {}
function autoSaveNote()  {}
function saveNote()      { saveNewNote(); }
function noteDatePrev()  {}
function noteDateNext()  {}
function noteTodayJump() {}
function jumpToNote()    {}

// ==========================================================================
// PHASE 1: STREAKS WIDGET
// ==========================================================================

async function loadStreaks() {
  try {
    const res = await apiGet('/api/analytics/streaks');
    if (!res.data) return;
    const s = res.data;
    const widget = el('streakWidget');
    if (el('streakTaskVal'))  el('streakTaskVal').textContent  = s.taskStreak    || 0;
    if (el('streakWkVal'))    el('streakWkVal').textContent    = s.workoutStreak || 0;
    if (el('streakCalVal'))   el('streakCalVal').textContent   = s.calorieStreak || 0;
    if (el('streakNoteVal'))  el('streakNoteVal').textContent  = s.noteStreak    || 0;
    if (widget) widget.style.display = '';
  } catch (_) {}
}

// ==========================================================================
// PHASE 8: FOCUS MODE
// ==========================================================================

let _focusTimer          = null;
let _focusEndTime        = null;
let _focusSessionId      = null;

async function openFocusModal() {
  el('focusModal')?.classList.remove('hidden');
  el('focusPreStart').style.display = '';
  el('focusTimerUI').style.display  = 'none';
  // Populate pending tasks
  const taskSel = el('focusTaskLink');
  if (taskSel && taskSel.options.length <= 1) {
    try {
      const res = await apiGet('/api/tasks');
      const tasks = (res.data || []).filter(t =>
          t.status === 'PENDING' || t.status === 'IN_PROGRESS');
      taskSel.innerHTML = '<option value="">— No linked task —</option>'
          + tasks.slice(0, 30).map(t =>
              `<option value="${esc(t.id)}">${esc(t.title)}</option>`).join('');
    } catch (_) {}
  }
}

function closeFocusModal() {
  el('focusModal')?.classList.add('hidden');
}

async function startFocusSession() {
  const duration     = parseInt(el('focusDuration')?.value) || 25;
  const linkedTaskId = el('focusTaskLink')?.value || null;
  try {
    const res = await apiPost('/api/focus', { durationMinutes: duration, linkedTaskId });
    const session = res.data;
    _focusSessionId = session.id;
    _focusEndTime   = new Date(session.endTime);
    el('focusPreStart').style.display = 'none';
    el('focusTimerUI').style.display  = '';
    _startFocusCountdown();
    toast('Focus session started! Get in the zone 🎯', 'success');
  } catch (e) {
    toast(e.message || 'Failed to start focus session', 'error');
  }
}

function _startFocusCountdown() {
  if (_focusTimer) clearInterval(_focusTimer);
  _focusTimer = setInterval(() => {
    const remaining = Math.max(0, Math.round((_focusEndTime - Date.now()) / 1000));
    const m = Math.floor(remaining / 60);
    const s = remaining % 60;
    if (el('focusTimerDisplay'))
      el('focusTimerDisplay').textContent = `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
    if (remaining === 0) {
      clearInterval(_focusTimer);
      _focusTimer = null;
      toast('Focus session complete! 🧠 Great work!', 'success');
      _onFocusComplete();
    }
  }, 1000);
}

async function endFocusEarly() {
  if (_focusSessionId) {
    try {
      await apiPost(`/api/focus/${_focusSessionId}/complete`, {});
    } catch (_) {}
  }
  _onFocusComplete();
}

function _onFocusComplete() {
  if (_focusTimer) clearInterval(_focusTimer);
  _focusTimer = null;
  _focusSessionId = null;
  closeFocusModal();
  loadStreaks();
  loadTasks();
}

// ==========================================================================
// PHASE 3: WEEKLY REPORT
// ==========================================================================

async function openWeeklyReport() {
  el('weeklyReportModal')?.classList.remove('hidden');
  el('weeklyReportBody').innerHTML = '<div class="loading">Generating report…</div>';
  try {
    const today = _localDateStr(new Date());
    const res   = await apiGet(`/api/analytics/weekly?weekDate=${today}`);
    const r     = res.data;
    if (!r) { el('weeklyReportBody').innerHTML = '<p>No data yet.</p>'; return; }
    el('weeklyReportBody').innerHTML = `
      <div class="wr-grid">
        <div class="wr-stat"><div class="wr-num">${r.totalTasksCompleted}</div><div class="wr-lbl">Tasks Done</div></div>
        <div class="wr-stat"><div class="wr-num">${r.totalWorkoutsCompleted}</div><div class="wr-lbl">Workouts</div></div>
        <div class="wr-stat"><div class="wr-num">${Math.round(r.consistencyPercent)}%</div><div class="wr-lbl">Consistency</div></div>
        <div class="wr-stat"><div class="wr-num">${r.currentStreak}</div><div class="wr-lbl">Best Streak</div></div>
      </div>
      <div class="wr-chart">
        ${(r.dailyScores || []).map(d => `
          <div class="wr-bar-wrap" title="${d.date}: ${d.score}">
            <div class="wr-bar" style="height:${Math.max(4,d.score)}%;background:${_scoreColor(d.score)}"></div>
            <div class="wr-bar-lbl">${d.date ? new Date(d.date).toLocaleDateString('en',{weekday:'short'}) : ''}</div>
          </div>`).join('')}
      </div>
      ${r.highestProductivityDay ? `<p class="wr-note">🏆 Best day: ${r.highestProductivityDay} (${r.highestDayScore}pts)</p>` : ''}
      ${r.lowestProductivityDay  ? `<p class="wr-note">📉 Lowest: ${r.lowestProductivityDay}</p>` : ''}`;
  } catch (e) {
    el('weeklyReportBody').innerHTML = `<p class="loading">${esc(e.message)}</p>`;
  }
}

function closeWeeklyReport() {
  el('weeklyReportModal')?.classList.add('hidden');
}

function _scoreColor(score) {
  if (score >= 80) return '#22c55e';
  if (score >= 50) return '#eab308';
  return '#ef4444';
}

// ==========================================================================
// PHASE 10: ACHIEVEMENT TOAST
// ==========================================================================

function showAchievementToast(name, icon) {
  const toast = el('achievementToast');
  if (!toast) return;
  if (el('achievementToastIcon')) el('achievementToastIcon').textContent = icon || '🏆';
  if (el('achievementToastName')) el('achievementToastName').textContent = name || 'New Achievement';
  toast.classList.remove('hidden');
  setTimeout(() => toast.classList.add('hidden'), 4000);
}

// ==========================================================================
// DAILY RECURRING TASKS
// ==========================================================================

function toggleDailyRoutines() {
  const panel = document.getElementById('dailyRoutinesSection');
  const btn   = document.getElementById('dailyRoutineStreakBtn');
  if (!panel) return;
  const isOpen = panel.style.display !== 'none';
  panel.style.display = isOpen ? 'none' : 'block';
  if (btn) btn.style.opacity = isOpen ? '1' : '0.65';
}

function toggleRecurringTime() {
  const cb  = document.getElementById('taskRecurring');
  const grp = document.getElementById('recurringTimeGroup');
  if (grp) grp.style.display = cb && cb.checked ? 'block' : 'none';
}

async function loadRecurringTasks() {
  try {
    const res   = await apiGet('/api/tasks/recurring');
    const tasks = res.data || [];
    const panel  = document.getElementById('dailyRoutinesSection');
    const list   = document.getElementById('dailyRoutinesList');
    const badge  = document.getElementById('dailyRoutinesCount');
    const btn    = document.getElementById('dailyRoutineStreakBtn');
    if (!list) return;
    if (!tasks.length) {
      if (btn)   btn.style.display = 'none';
      if (panel) panel.style.display = 'none';
      return;
    }
    // Show the button in the streak bar
    if (btn) { btn.style.display = 'inline-flex'; }
    if (badge) badge.textContent = tasks.length;
    // Keep panel closed on refresh unless already open
    const isOpen = panel && panel.style.display !== 'none';
    if (panel && !isOpen) panel.style.display = 'none';
    const sLbl = { PENDING: '⭕ Pending', IN_PROGRESS: '🔄 In Progress', DONE: '✅ Done', COMPLETED: '✅ Done' };
    list.innerHTML = tasks.map(t => `
      <div class="task-card" style="margin-bottom:0.5rem;border-left:3px solid #a78bfa">
        <div class="task-card-header">
          <span class="task-title">⏰ ${esc(t.title)}</span>
          <span class="task-status status-${(t.status||'pending').toLowerCase()}">${sLbl[t.status] || t.status}</span>
        </div>
        <div class="task-meta">
          <span>Daily @ <strong>${esc(t.recurringTime || '--:--')}</strong></span>
          ${t.category ? `<span>${esc(t.category)}</span>` : ''}
          ${t.collaboratorIds && t.collaboratorIds.length ? `<span>👥 ${t.collaboratorIds.length} collab(s)</span>` : ''}
        </div>
        <div style="display:flex;gap:0.4rem;padding:0.25rem 0.5rem 0.35rem">
          <button class="btn-ghost btn-xs" onclick="collabCycleTaskStatus('${t.id}','${t.status||'PENDING'}')">🔄 Cycle Status</button>
          <button class="btn-ghost btn-xs" onclick="openEditTask('${t.id}')">✏️ Edit</button>
        </div>
      </div>`).join('');
  } catch (_err) { /* silent */ }
}

// ==========================================================================
// COLLABORATOR PROGRESS
// ==========================================================================

function openProgressModal(taskId, currentPct, currentNote) {
  const pidEl  = document.getElementById('progressTaskId');
  const pctEl  = document.getElementById('progressPct');
  const noteEl = document.getElementById('progressNote');
  if (pidEl)  pidEl.value  = taskId;
  if (pctEl)  pctEl.value  = currentPct != null ? currentPct : 0;
  if (noteEl) noteEl.value = currentNote || '';
  openModal('collabProgressModal');
}

async function submitCollabProgress() {
  const taskId = document.getElementById('progressTaskId')?.value;
  const pct    = parseInt(document.getElementById('progressPct')?.value || '0', 10);
  const note   = document.getElementById('progressNote')?.value?.trim() || '';
  if (!taskId) return;
  try {
    await apiPatch(`/api/tasks/${taskId}/my-progress`, { completionPct: pct, note });
    document.getElementById('collabProgressModal').classList.add('hidden');
    toast('Progress saved ✅', 'success');
    loadCollabTasks();
  } catch (err) { toast(err.message || 'Failed to save progress', 'error'); }
}

// ==========================================================================
// SESSION TRACKER
// ==========================================================================
let _sessions = [];

async function loadSessions() {
  const list = document.getElementById('sessionList');
  if (!list) return;
  list.innerHTML = '<div class="loading">Loading sessions…</div>';
  try {
    const data = await apiGet('/api/sessions');
    _sessions = data || [];
    renderSessions();
    // Prefill today's date
    const dateInput = document.getElementById('sessionDate');
    if (dateInput && !dateInput.value) dateInput.value = new Date().toISOString().split('T')[0];
  } catch (e) {
    list.innerHTML = '<p style="color:var(--danger,#f87171);padding:0.5rem">Failed to load sessions.</p>';
  }
}

function renderSessions() {
  const list = document.getElementById('sessionList');
  if (!list) return;
  if (!_sessions.length) {
    list.innerHTML = `<div style="text-align:center;padding:2rem;opacity:.6">
      <div style="font-size:2.5rem">🏃</div>
      <div style="font-weight:600;margin:.5rem 0">No sessions logged yet</div>
      <div style="font-size:.85rem">Use the form above or speak a session to get started.</div>
    </div>`;
    return;
  }
  list.innerHTML = _sessions.map(s => `
    <div style="background:var(--card-bg,#1e293b);border-radius:12px;padding:0.9rem 1rem;margin-bottom:0.6rem;display:flex;justify-content:space-between;align-items:flex-start">
      <div>
        <div style="font-weight:700;font-size:0.95rem">${esc(s.type || 'Workout')}</div>
        <div style="font-size:0.8rem;opacity:.7;margin-top:2px">
          ${s.sessionDate || ''}
          ${s.durationMinutes ? ' · <b>' + s.durationMinutes + '</b> min' : ''}
          ${s.caloriesBurned ? ' · <b>' + s.caloriesBurned + '</b> kcal' : ''}
          ${s.mood ? ' · ' + esc(s.mood) : ''}
        </div>
        ${s.notes ? `<div style="font-size:0.8rem;margin-top:4px;opacity:.6">${esc(s.notes)}</div>` : ''}
      </div>
      <button class="btn-icon" onclick="deleteSession('${s.id}')" title="Delete" style="flex-shrink:0">🗑</button>
    </div>`).join('');
}

async function submitSession(e) {
  e.preventDefault();
  const type     = document.getElementById('sessionType')?.value?.trim();
  const duration = document.getElementById('sessionDuration')?.value;
  const calories = document.getElementById('sessionCalories')?.value;
  const mood     = document.getElementById('sessionMood')?.value?.trim();
  const notes    = document.getElementById('sessionNotes')?.value?.trim();
  const date     = document.getElementById('sessionDate')?.value;
  if (!type) { toast('Please enter a session type', 'error'); return; }
  try {
    await apiPost('/api/sessions', {
      type,
      durationMinutes: duration ? parseInt(duration) : null,
      caloriesBurned: calories ? parseInt(calories) : null,
      mood: mood || null,
      notes: notes || null,
      sessionDate: date || null
    });
    document.getElementById('sessionForm').reset();
    document.getElementById('sessionDate').value = new Date().toISOString().split('T')[0];
    toast('Session saved!', 'success');
    aiSuggest(`Just completed a ${type || 'fitness'} session. Give a recovery or motivation tip.`);
    loadSessions();
  } catch (err) {
    toast(err.message || 'Failed to save session', 'error');
  }
}

async function deleteSession(id) {
  if (!confirm('Delete this session?')) return;
  try {
    await apiDelete(`/api/sessions/${id}`);
    toast('Session deleted', 'success');
    loadSessions();
  } catch (err) {
    toast(err.message || 'Failed to delete', 'error');
  }
}

// Voice Session Logger — uses Web Speech API + Groq AI to parse spoken input
function startSessionVoice() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) { toast('Speech recognition not supported in this browser', 'error'); return; }
  const btn = document.getElementById('sessionVoiceBtn');
  const status = document.getElementById('sessionVoiceStatus');
  const rec = new SpeechRecognition();
  rec.lang = 'en-US';
  rec.interimResults = false;
  rec.maxAlternatives = 1;
  btn.textContent = '⏹ Listening…';
  btn.style.background = 'var(--danger, #f87171)';
  if (status) status.textContent = 'Listening… speak your session (e.g. "45 min gym, 300 calories, felt energized")';
  rec.start();
  rec.onresult = async (event) => {
    const transcript = event.results[0][0].transcript;
    if (status) status.textContent = `Heard: "${transcript}" — parsing…`;
    btn.textContent = '🎤 Speak Session';
    btn.style.background = '';
    await parseSessionWithAI(transcript);
  };
  rec.onerror = () => {
    btn.textContent = '🎤 Speak Session';
    btn.style.background = '';
    if (status) status.textContent = '';
    toast('Could not hear you, please try again', 'error');
  };
  rec.onend = () => {
    btn.textContent = '🎤 Speak Session';
    btn.style.background = '';
  };
}

async function parseSessionWithAI(transcript) {
  const status = document.getElementById('sessionVoiceStatus');
  try {
    const resp = await apiPost('/api/ai/chat', {
      message: `Extract workout session details from this spoken input and return ONLY a JSON object with fields: type (string), durationMinutes (number or null), caloriesBurned (number or null), mood (string or null), notes (string or null). Spoken input: "${transcript}"`
    });
    let text = resp?.message || resp?.reply || '';
    // Strip markdown code fences if present
    text = text.replace(/```json\s*/gi, '').replace(/```/g, '').trim();
    const parsed = JSON.parse(text);
    if (parsed.type) document.getElementById('sessionType').value = parsed.type;
    if (parsed.durationMinutes) document.getElementById('sessionDuration').value = parsed.durationMinutes;
    if (parsed.caloriesBurned) document.getElementById('sessionCalories').value = parsed.caloriesBurned;
    if (parsed.mood) document.getElementById('sessionMood').value = parsed.mood;
    if (parsed.notes) document.getElementById('sessionNotes').value = parsed.notes;
    if (status) status.textContent = '✅ Form filled — review and save!';
    toast('Session details extracted!', 'success');
  } catch (e) {
    // Fallback: just fill notes with the transcript
    document.getElementById('sessionNotes').value = transcript;
    if (status) status.textContent = '⚠ Could not parse — transcript added to notes.';
  }
}

// ==========================================================================
// PROGRESS PHOTO GALLERY
// ==========================================================================
let _progressPhotos = [];

async function loadProgressPhotos() {
  const grid = document.getElementById('photoGalleryGrid');
  if (!grid) return;
  grid.innerHTML = '<div class="loading" style="grid-column:1/-1">Loading photos…</div>';
  // Prefill today date
  const dateInput = document.getElementById('photoDate');
  if (dateInput && !dateInput.value) dateInput.value = new Date().toISOString().split('T')[0];
  try {
    const data = await apiGet('/api/progress-photos');
    _progressPhotos = data || [];
    renderProgressPhotos();
  } catch (e) {
    grid.innerHTML = '<p style="color:var(--danger,#f87171);grid-column:1/-1">Failed to load photos.</p>';
  }
}

function renderProgressPhotos() {
  const grid = document.getElementById('photoGalleryGrid');
  if (!grid) return;
  if (!_progressPhotos.length) {
    grid.innerHTML = `<div style="grid-column:1/-1;text-align:center;padding:2rem;opacity:.6">
      <div style="font-size:2.5rem">📸</div>
      <div style="font-weight:600;margin:.5rem 0">No progress photos yet</div>
      <div style="font-size:.85rem">Upload your first check-in photo above!</div>
    </div>`;
    return;
  }
  grid.innerHTML = _progressPhotos.map(p => `
    <div style="position:relative;border-radius:12px;overflow:hidden;background:var(--card-bg,#1e293b);aspect-ratio:1/1">
      <img src="${p.previewUrl}" alt="progress photo"
           style="width:100%;height:100%;object-fit:cover;display:block"
           onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 1 1%22/>'"/>
      <div style="position:absolute;bottom:0;left:0;right:0;background:rgba(0,0,0,.65);padding:0.4rem 0.5rem;font-size:0.7rem">
        <div style="font-weight:600">${p.photoDate || ''}</div>
        ${p.notes ? `<div style="opacity:.8;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(p.notes)}</div>` : ''}
      </div>
      <button onclick="deleteProgressPhoto('${p.id}')"
              style="position:absolute;top:6px;right:6px;background:rgba(0,0,0,.6);border:none;border-radius:50%;width:26px;height:26px;cursor:pointer;font-size:0.75rem;color:#fff">🗑</button>
    </div>`).join('');
}

async function uploadProgressPhoto(e) {
  e.preventDefault();
  const fileInput = document.getElementById('photoFile');
  const date = document.getElementById('photoDate')?.value;
  const notes = document.getElementById('photoNotes')?.value?.trim();
  if (!fileInput?.files?.length) { toast('Please select a photo', 'error'); return; }
  const btn = document.getElementById('photoUploadBtn');
  if (btn) { btn.disabled = true; btn.textContent = 'Uploading…'; }
  const formData = new FormData();
  formData.append('file', fileInput.files[0]);
  if (notes) formData.append('notes', notes);
  if (date) formData.append('photoDate', date);
  try {
    const token = localStorage.getItem('authToken') || localStorage.getItem('token') || '';
    const resp = await fetch('/api/progress-photos', {
      method: 'POST',
      headers: token ? { 'Authorization': 'Bearer ' + token } : {},
      body: formData
    });
    if (!resp.ok) throw new Error('Upload failed');
    document.getElementById('photoUploadForm').reset();
    document.getElementById('photoDate').value = new Date().toISOString().split('T')[0];
    toast('Photo uploaded!', 'success');
    loadProgressPhotos();
  } catch (err) {
    toast(err.message || 'Upload failed', 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '⬆ Upload Photo'; }
  }
}

async function deleteProgressPhoto(id) {
  if (!confirm('Delete this photo?')) return;
  try {
    await apiDelete(`/api/progress-photos/${id}`);
    toast('Photo deleted', 'success');
    loadProgressPhotos();
  } catch (err) {
    toast(err.message || 'Failed to delete', 'error');
  }
}

// ==========================================================================
// BRAIN COACH
// ==========================================================================
function initBrainCoachTab() {
  loadChallenges();
}

// ---- Challenges ----
let _challenges = [];

async function loadChallenges() {
  const list = document.getElementById('challengeList');
  if (!list) return;
  list.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res = await apiGet('/api/brain-coach/challenges');
    console.log('[BrainCoach] loadChallenges response:', res);
    const raw = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
    // Deduplicate by question text — keep the first occurrence (latest by API order)
    const seen = new Set();
    _challenges = raw.filter(c => {
      const key = (c.question || '').trim().toLowerCase();
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
    renderChallenges();
  } catch (e) {
    console.error('[BrainCoach] loadChallenges error:', e);
    list.innerHTML = '<p style="color:var(--danger,#f87171);padding:.5rem">Failed to load challenges.</p>';
  }
}

function renderChallenges() {
  const list = document.getElementById('challengeList');
  if (!list) return;
  if (!_challenges.length) {
    list.innerHTML = `<div style="text-align:center;padding:2rem;opacity:.6">
      <div style="font-size:2.5rem">🎯</div>
      <div style="font-weight:600;margin:.5rem 0">No challenges yet</div>
      <div style="font-size:.85rem">Click "New Challenge" to get started!</div>
    </div>`;
    return;
  }
  list.innerHTML = _challenges.map(c => `
    <div style="background:var(--card-bg,#1e293b);border-radius:12px;padding:1rem;border-left:3px solid ${c.correct===true?'#22c55e':c.correct===false?'#f87171':'var(--primary,#6366f1)'}">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div style="flex:1">
          <div style="font-size:0.7rem;opacity:.6;margin-bottom:.4rem">${esc(c.category||'')} · ${esc(c.difficulty||'')} ${c.correct===true?'✅':c.correct===false?'❌':''}</div>
          <div style="font-weight:600;margin-bottom:.5rem">${esc(c.question||'')}</div>
          ${c.hint ? `<details style="font-size:.8rem;opacity:.7;margin-bottom:.5rem"><summary style="cursor:pointer">💡 Hint</summary>${esc(c.hint)}</details>` : ''}
          ${c.userAnswer
            ? `<div style="font-size:.85rem;margin-top:.4rem">Your answer: <b>${esc(c.userAnswer)}</b><br/>Correct answer: <b>${esc(c.answer||'')}</b></div>`
            : `<div style="display:flex;gap:.5rem;margin-top:.5rem">
                <input id="ans-${c.id}" class="form-input" style="flex:1" placeholder="Your answer…"/>
                <button class="btn-primary btn-sm" onclick="submitChallengeAnswer('${c.id}')">Submit</button>
              </div>`
          }
        </div>
        <button class="btn-icon" onclick="deleteChallenge('${c.id}')" title="Delete" style="flex-shrink:0;margin-left:.5rem">🗑</button>
      </div>
    </div>`).join('');
}

async function generateChallenge() {
  const category   = document.getElementById('challengeCategory')?.value;
  const difficulty = document.getElementById('challengeDifficulty')?.value;
  const list = document.getElementById('challengeList');
  console.log('[BrainCoach] Generating challenge...', { category, difficulty });
  try {
    toast('Generating challenge…', 'info');
    if (list) list.innerHTML = '<div class="loading">Generating challenge…</div>';
    const res = await apiPost('/api/brain-coach/challenges/generate', { category, difficulty });
    console.log('[BrainCoach] Challenge API response:', res);
    await loadChallenges();
  } catch (err) {
    console.error('[BrainCoach] Challenge generation failed:', err);
    toast('Challenge generation failed. Try again.', 'error');
    if (list) list.innerHTML = '<p style="color:var(--danger,#f87171);padding:.5rem;text-align:center">Challenge generation failed. Try again.</p>';
  }
}

async function submitChallengeAnswer(id) {
  const input = document.getElementById(`ans-${id}`);
  const answer = input?.value?.trim();
  if (!answer) { toast('Please enter an answer', 'error'); return; }
  try {
    await apiPost(`/api/brain-coach/challenges/${id}/answer`, { answer });
    loadChallenges();
  } catch (err) {
    toast(err.message || 'Failed to submit', 'error');
  }
}

async function deleteChallenge(id) {
  if (!confirm('Delete this challenge?')) return;
  try {
    await apiDelete(`/api/brain-coach/challenges/${id}`);
    toast('Challenge deleted', 'success');
    loadChallenges();
  } catch (err) {
    toast(err.message || 'Failed to delete', 'error');
  }
}

// ---- Decision Log ----
let _decisions = [];

async function loadDecisionLogs() {
  const list = document.getElementById('decisionList');
  if (!list) return;
  list.innerHTML = '<div class="loading">Loading…</div>';
  try {
    const res = await apiGet('/api/brain-coach/decisions');
    _decisions = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
    renderDecisionLogs();
  } catch (e) {
    list.innerHTML = '<p style="color:var(--danger,#f87171);padding:.5rem">Failed to load journal.</p>';
  }
}

function renderDecisionLogs() {
  const list = document.getElementById('decisionList');
  if (!list) return;
  if (!_decisions.length) {
    list.innerHTML = `<div style="text-align:center;padding:2rem;opacity:.6">
      <div style="font-size:2.5rem">📓</div>
      <div style="font-weight:600;margin:.5rem 0">No journal entries yet</div>
      <div style="font-size:.85rem">Record a decision above to get AI reflection.</div>
    </div>`;
    return;
  }
  list.innerHTML = _decisions.map(d => `
    <div style="background:var(--card-bg,#1e293b);border-radius:12px;padding:1rem">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div style="flex:1">
          <div style="font-size:.7rem;opacity:.6;margin-bottom:.3rem">${d.decisionDate||''}</div>
          <div style="font-weight:700;margin-bottom:.4rem">${esc(d.decision||'')}</div>
          ${d.context ? `<div style="font-size:.82rem;opacity:.7;margin-bottom:.4rem">Context: ${esc(d.context)}</div>` : ''}
          ${d.aiReflection ? `<div style="font-size:.82rem;border-left:3px solid var(--primary,#6366f1);padding-left:.6rem;margin:.5rem 0;opacity:.85">🤖 ${esc(d.aiReflection)}</div>` : ''}
          ${d.outcome
            ? `<div style="font-size:.82rem;margin-top:.4rem;opacity:.8">Outcome: <b>${esc(d.outcome)}</b></div>`
            : `<div style="display:flex;gap:.5rem;margin-top:.5rem">
                <input id="outcome-${d.id}" class="form-input" style="flex:1" placeholder="What was the outcome?"/>
                <button class="btn-secondary btn-sm" onclick="saveDecisionOutcome('${d.id}')">Save Outcome</button>
              </div>`
          }
        </div>
        <button class="btn-icon" onclick="deleteDecisionLog('${d.id}')" title="Delete" style="flex-shrink:0;margin-left:.5rem">🗑</button>
      </div>
    </div>`).join('');
}

async function submitDecision(e) {
  e.preventDefault();
  const decision = document.getElementById('decisionText')?.value?.trim();
  const context  = document.getElementById('decisionContext')?.value?.trim();
  if (!decision) { toast('Please enter a decision', 'error'); return; }
  try {
    toast('Saving and asking AI…', 'info');
    await apiPost('/api/brain-coach/decisions', { decision, context: context || '' });
    document.getElementById('decisionForm').reset();
    toast('Decision saved!', 'success');
    loadDecisionLogs();
  } catch (err) {
    toast(err.message || 'Failed to save', 'error');
  }
}

async function saveDecisionOutcome(id) {
  const input = document.getElementById(`outcome-${id}`);
  const outcome = input?.value?.trim();
  if (!outcome) { toast('Please enter an outcome', 'error'); return; }
  try {
    await apiPatch(`/api/brain-coach/decisions/${id}/outcome`, { outcome });
    toast('Outcome saved!', 'success');
    loadDecisionLogs();
  } catch (err) {
    toast(err.message || 'Failed to save', 'error');
  }
}

async function deleteDecisionLog(id) {
  if (!confirm('Delete this journal entry?')) return;
  try {
    await apiDelete(`/api/brain-coach/decisions/${id}`);
    toast('Entry deleted', 'success');
    loadDecisionLogs();
  } catch (err) {
    toast(err.message || 'Failed to delete', 'error');
  }
}

// ==========================================================================
// STEP 9: AI SUGGESTIONS (non-intrusive tips)
// ==========================================================================
async function aiSuggest(context) {
  try {
    const data = await apiPost('/api/ai/suggest', { context });
    const tip = data?.tip || data?.data || data;
    if (typeof tip === 'string' && tip.trim()) {
      showAiTip(tip.trim());
    }
  } catch (_) { /* silent – suggestions are best-effort */ }
}

function showAiTip(text) {
  let el = document.getElementById('aiTipBanner');
  if (!el) {
    el = document.createElement('div');
    el.id = 'aiTipBanner';
    el.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);' +
      'background:var(--primary,#6366f1);color:#fff;padding:.65rem 1.2rem;border-radius:12px;' +
      'font-size:.82rem;max-width:90vw;z-index:2000;box-shadow:0 4px 16px rgba(0,0,0,.35);' +
      'display:flex;gap:.75rem;align-items:center';
    el.innerHTML = '<span id="aiTipText"></span><button onclick="this.parentElement.remove()" style="background:transparent;border:none;color:#fff;font-size:1.1rem;cursor:pointer;line-height:1">×</button>';
    document.body.appendChild(el);
  }
  document.getElementById('aiTipText').textContent = '🤖 ' + text;
  el.style.display = 'flex';
  clearTimeout(el._timer);
  el._timer = setTimeout(() => { if (el.parentElement) el.remove(); }, 8000);
}

// ==========================================================================
// STEP 9: AI SUGGESTIONS (non-intrusive tips)
// ==========================================================================
async function aiSuggest(context) {
  try {
    const data = await apiPost('/api/ai/suggest', { context });
    const tip = data?.tip || data?.data || data;
    if (typeof tip === 'string' && tip.trim()) {
      showAiTip(tip.trim());
    }
  } catch (_) { /* silent – suggestions are best-effort */ }
}

function showAiTip(text) {
  let el = document.getElementById('aiTipBanner');
  if (!el) {
    el = document.createElement('div');
    el.id = 'aiTipBanner';
    el.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);' +
      'background:var(--primary,#6366f1);color:#fff;padding:.65rem 1.2rem;border-radius:12px;' +
      'font-size:.82rem;max-width:90vw;z-index:2000;box-shadow:0 4px 16px rgba(0,0,0,.35);' +
      'display:flex;gap:.75rem;align-items:center';
    el.innerHTML = '<span id="aiTipText"></span><button onclick="this.parentElement.remove()" style="background:transparent;border:none;color:#fff;font-size:1.1rem;cursor:pointer;line-height:1">×</button>';
    document.body.appendChild(el);
  }
  document.getElementById('aiTipText').textContent = '🤖 ' + text;
  el.style.display = 'flex';
  clearTimeout(el._timer);
  el._timer = setTimeout(() => { if (el.parentElement) el.remove(); }, 8000);
}
