/* ============================================================
   alarm-overlay.js — Fullscreen alarm overlay + Web Audio beeper
   Polls /api/alarms/pending every 30 s.
   When an alarm is due it shows a black fullscreen overlay,
   plays a Web Audio beeper, and shows a red STOP button.
   ============================================================ */

(function () {
  'use strict';

  // ---------- Audio ----------------------------------------------------------

  let _audioCtx = null;
  let _beepInterval = null;

  function getAudioCtx() {
    if (!_audioCtx) {
      _audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    return _audioCtx;
  }

  function beep(freq, duration) {
    try {
      const ctx = getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = freq || 880;
      osc.type = 'square';
      gain.gain.setValueAtTime(0.35, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + (duration || 0.4));
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + (duration || 0.4));
    } catch (e) { /* AudioContext not available */ }
  }

  function startBeeping() {
    beep(880, 0.3);
    _beepInterval = setInterval(() => beep(880, 0.3), 900);
  }

  function stopBeeping() {
    if (_beepInterval) { clearInterval(_beepInterval); _beepInterval = null; }
  }

  // ---------- Overlay --------------------------------------------------------

  let _currentAlarmId = null;

  function showAlarmOverlay(alarm) {
    if (document.getElementById('alarmOverlay')) return; // already showing

    _currentAlarmId = alarm.id;
    startBeeping();

    const overlay = document.createElement('div');
    overlay.id = 'alarmOverlay';

    const timeStr = alarm.scheduledAt
      ? new Date(alarm.scheduledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : '';

    overlay.innerHTML = `
      <div class="alarm-icon">⏰</div>
      <div class="alarm-title">${escAlarm(alarm.title || 'Alarm')}</div>
      ${timeStr ? `<div class="alarm-time">${timeStr}</div>` : ''}
      <button class="alarm-stop-btn" id="alarmStopBtn">■ STOP</button>
    `;

    document.body.appendChild(overlay);

    document.getElementById('alarmStopBtn').addEventListener('click', dismissAlarm);
  }

  function escAlarm(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  function dismissAlarm() {
    stopBeeping();
    const overlay = document.getElementById('alarmOverlay');
    if (overlay) overlay.remove();

    if (_currentAlarmId) {
      const token = localStorage.getItem('token');
      if (token) {
        fetch(`/api/alarms/${_currentAlarmId}/dismiss`, {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + token }
        }).catch(() => {});
      }
      _currentAlarmId = null;
    }
  }

  // ---------- Polling --------------------------------------------------------

  async function pollAlarms() {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const res = await fetch('/api/alarms/pending', {
        headers: { 'Authorization': 'Bearer ' + token }
      });
      if (!res.ok) return;
      const json = await res.json();
      const alarms = (json.data || []);
      if (alarms.length > 0) {
        // Show the first alarm; the next one will show after STOP is pressed
        showAlarmOverlay(alarms[0]);
      }
    } catch (_) { /* network error — ignore */ }
  }

  // Start polling 5 s after page load, then every 30 s
  window.addEventListener('load', () => {
    setTimeout(() => {
      pollAlarms();
      setInterval(pollAlarms, 30_000);
    }, 5000);
  });

  // Expose globally so other code can trigger an alarm overlay directly
  window.showAlarmOverlay = showAlarmOverlay;
  window.dismissAlarmOverlay = dismissAlarm;
})();
