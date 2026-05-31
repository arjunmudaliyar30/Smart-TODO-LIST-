/* ============================================================
   voice-commander.js — Global floating mic button
   Web Speech API → keyword intent detection → action
   Groq AI fallback for unclear commands
   Web Speech Synthesis for audio feedback
   ============================================================ */

(function () {
  'use strict';

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) return; // browser doesn't support — silently skip

  // ---------- Inject floating mic button ---------------------------------

  const fab = document.createElement('button');
  fab.id = 'voiceFab';
  fab.title = 'Voice Command';
  fab.setAttribute('aria-label', 'Voice Command');
  fab.innerHTML = '🎙️';
  fab.style.cssText = [
    'position:fixed',
    'bottom:5rem',
    'right:1rem',
    'z-index:9000',
    'width:3rem',
    'height:3rem',
    'border-radius:50%',
    'border:none',
    'background:var(--accent,#6c63ff)',
    'color:#fff',
    'font-size:1.3rem',
    'cursor:pointer',
    'box-shadow:0 4px 14px rgba(108,99,255,0.5)',
    'transition:transform 0.15s,box-shadow 0.15s',
    'display:flex',
    'align-items:center',
    'justify-content:center',
  ].join(';');

  document.body.appendChild(fab);

  // ---------- Speech Recognition -----------------------------------------

  const recognition = new SpeechRecognition();
  recognition.lang = 'en-US';
  recognition.interimResults = true;   // show partial results for better UX
  recognition.continuous = true;       // keep listening until user taps stop
  recognition.maxAlternatives = 1;

  let _listening = false;
  let _finalTranscript = '';
  let _silenceTimer = null;

  // Visual indicator overlay for active listening
  let _listenBubble = null;
  function _showListenBubble(text) {
    if (!_listenBubble) {
      _listenBubble = document.createElement('div');
      _listenBubble.style.cssText = [
        'position:fixed','bottom:9rem','left:50%','transform:translateX(-50%)',
        'background:#1a1a2e','border:1px solid rgba(108,99,255,0.4)',
        'border-radius:20px','padding:0.5rem 1.1rem','max-width:80vw',
        'font-size:0.85rem','color:#c0c0e0','z-index:10000',
        'pointer-events:none','text-align:center','transition:opacity 0.2s'
      ].join(';');
      document.body.appendChild(_listenBubble);
    }
    _listenBubble.textContent = text || '🎙 Listening…';
    _listenBubble.style.opacity = '1';
  }
  function _hideListenBubble() {
    if (_listenBubble) _listenBubble.style.opacity = '0';
  }

  fab.addEventListener('click', () => {
    if (_listening) {
      clearTimeout(_silenceTimer);
      recognition.stop();
    } else {
      _finalTranscript = '';
      try {
        recognition.start();
      } catch (e) { /* already started */ }
    }
  });

  recognition.onstart = () => {
    _listening = true;
    _finalTranscript = '';
    fab.style.background = '#ef4444';
    fab.style.boxShadow = '0 0 20px rgba(239,68,68,0.8)';
    fab.innerHTML = '⏹';
    _showListenBubble('🎙 Listening… tap to stop');
    speak('Listening');
  };

  recognition.onend = () => {
    _listening = false;
    clearTimeout(_silenceTimer);
    fab.style.background = 'var(--accent,#6c63ff)';
    fab.style.boxShadow = '0 4px 14px rgba(108,99,255,0.5)';
    fab.innerHTML = '🎙️';
    _hideListenBubble();
    if (_finalTranscript.trim()) {
      handleCommand(_finalTranscript.trim());
    }
    _finalTranscript = '';
  };

  recognition.onerror = (e) => {
    _listening = false;
    clearTimeout(_silenceTimer);
    fab.style.background = 'var(--accent,#6c63ff)';
    fab.innerHTML = '🎙️';
    _hideListenBubble();
    if (e.error !== 'no-speech' && e.error !== 'aborted') {
      speak('Sorry, I did not catch that');
    }
    _finalTranscript = '';
  };

  recognition.onresult = (event) => {
    let interim = '';
    _finalTranscript = '';
    for (let i = 0; i < event.results.length; i++) {
      if (event.results[i].isFinal) {
        _finalTranscript += event.results[i][0].transcript + ' ';
      } else {
        interim += event.results[i][0].transcript;
      }
    }
    // Show live preview in bubble
    _showListenBubble('🎙 ' + (_finalTranscript + interim).trim() + '…');
    // Auto-stop after 1.5s of silence (no new results)
    clearTimeout(_silenceTimer);
    _silenceTimer = setTimeout(() => {
      if (_listening) recognition.stop();
    }, 1500);
  };

  // ---------- Keyword Intent Detection -----------------------------------

  const INTENTS = [
    {
      pattern: /\badd task\b|\bcreate task\b|\bnew task\b|\bschedule task\b/,
      action: () => {
        if (typeof document.getElementById('openTaskModal')?.click === 'function') {
          document.getElementById('openTaskModal').click();
          speak('Opening new task form');
        }
      }
    },
    {
      pattern: /\badd goal\b|\bcreate goal\b|\bnew goal\b/,
      action: () => {
        if (typeof window.openModal === 'function') {
          window.openModal('goalModal');
          speak('Opening new goal form');
        }
      }
    },
    {
      pattern: /\bopen focus\b|\bstart focus\b|\bfocus mode\b/,
      action: () => {
        if (typeof window.openFocusModal === 'function') {
          window.openFocusModal();
          speak('Starting focus mode');
        }
      }
    },
    {
      pattern: /\bopen fitness\b|\bgo to fitness\b|\bfitness tab\b/,
      action: () => {
        const tab = document.querySelector('[data-tab="fitness"]') ||
                    document.querySelector('[onclick*="fitness"]');
        if (tab) { tab.click(); speak('Opening fitness'); }
      }
    },
    {
      pattern: /\bopen tasks\b|\bgo to tasks\b|\btasks tab\b/,
      action: () => {
        const tab = document.querySelector('[data-tab="tasks"]') ||
                    document.querySelector('[onclick*="tasks"]');
        if (tab) { tab.click(); speak('Opening tasks'); }
      }
    },
    {
      pattern: /\bopen goals\b|\bgo to goals\b|\bgoals tab\b/,
      action: () => {
        const tab = document.querySelector('[data-tab="goals"]') ||
                    document.querySelector('[onclick*="goals"]');
        if (tab) { tab.click(); speak('Opening goals'); }
      }
    },
    {
      pattern: /\bweekly report\b|\bshow report\b|\bmy report\b/,
      action: () => {
        if (typeof window.openWeeklyReport === 'function') {
          window.openWeeklyReport();
          speak('Opening weekly report');
        }
      }
    },
    {
      pattern: /\blogout\b|\bsign out\b/,
      action: () => {
        speak('Logging out');
        setTimeout(() => {
          const btn = document.getElementById('logoutBtn');
          if (btn) btn.click();
        }, 1200);
      }
    },
    {
      pattern: /\bstop alarm\b|\bdismiss alarm\b/,
      action: () => {
        if (typeof window.dismissAlarmOverlay === 'function') {
          window.dismissAlarmOverlay();
          speak('Alarm dismissed');
        }
      }
    }
  ];

  function handleCommand(transcript) {
    const lower = transcript.toLowerCase();
    for (const intent of INTENTS) {
      if (intent.pattern.test(lower)) {
        intent.action(transcript);
        return;
      }
    }
    // No keyword match — always fall through to AI for natural-language scheduling
    sendToAI(transcript);
  }

  // ---------- Groq AI Fallback -------------------------------------------

  async function sendToAI(transcript) {
    _showListenBubble('🤖 Processing…');
    speak('Let me think');
    const token = localStorage.getItem('token');
    if (!token) { speak('Please log in first'); _hideListenBubble(); return; }

    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ message: transcript })
      });
      if (!res.ok) { speak('AI is unavailable right now'); _hideListenBubble(); return; }
      const json = await res.json();
      const data  = json?.data;
      const reply = data?.message?.content || 'Done';

      // Reload affected lists
      if (data?.taskCreated || (data?.tasksCreated && data.tasksCreated.length)) {
        if (typeof window.loadTasks === 'function') window.loadTasks();
      }
      if (data?.goalCreated   && typeof window.loadGoals    === 'function') window.loadGoals();
      if (data?.workoutCreated && typeof window.loadWorkouts === 'function') window.loadWorkouts();

      // Count what was created for a nice spoken summary
      const taskCount = data?.tasksCreated?.length || (data?.taskCreated ? 1 : 0);
      let spokenReply = reply.slice(0, 200);
      if (taskCount > 1) spokenReply = `Created ${taskCount} tasks: ` + data.tasksCreated.map(t => t.title).join(', ');
      else if (taskCount === 1) spokenReply = `Scheduled: ${(data.taskCreated || data.tasksCreated[0]).title}`;

      speak(spokenReply);
      if (typeof window.toast === 'function') window.toast('🎙️ ' + reply.slice(0, 140));

    } catch (_) {
      speak('Something went wrong');
    } finally {
      _hideListenBubble();
    }
  }

  // ---------- Speech Synthesis -------------------------------------------

  // Cache a consistent English voice so it sounds the same on all devices
  let _preferredVoice = null;

  function _loadPreferredVoice() {
    const voices = window.speechSynthesis.getVoices();
    if (!voices.length) return;
    _preferredVoice =
      voices.find(v => /google us english/i.test(v.name)) ||
      voices.find(v => /microsoft zira/i.test(v.name)) ||
      voices.find(v => /microsoft david/i.test(v.name)) ||
      voices.find(v => v.lang === 'en-US' && !v.localService) ||  // online/cloud voice
      voices.find(v => v.lang === 'en-US') ||
      voices.find(v => v.lang.startsWith('en-')) ||
      null;
  }

  // Voices load asynchronously on Chrome/Android — listen for the event
  if (window.speechSynthesis) {
    _loadPreferredVoice();
    window.speechSynthesis.onvoiceschanged = _loadPreferredVoice;
  }

  function speak(text) {
    if (!window.speechSynthesis) return;
    window.speechSynthesis.cancel();
    const utt = new SpeechSynthesisUtterance(text);
    utt.lang = 'en-US';
    utt.rate = 1.1;
    utt.pitch = 1;
    utt.volume = 0.9;
    if (!_preferredVoice) _loadPreferredVoice();
    if (_preferredVoice) utt.voice = _preferredVoice;
    window.speechSynthesis.speak(utt);
  }

  // ---------- Step 4: Hide/move mic when People tab is active -----------

  function _updateMicPosition() {
    const peopleActive = document.querySelector('#tab-people.active');
    if (peopleActive) {
      fab.style.bottom = 'auto';
      fab.style.top = '0.75rem';
    } else {
      fab.style.bottom = '5rem';
      fab.style.top = 'auto';
    }
  }

  const _peopleTab = document.getElementById('tab-people');
  if (_peopleTab) {
    new MutationObserver(function() { _updateMicPosition(); })
      .observe(_peopleTab, { attributes: true, attributeFilter: ['class'] });
  }
  _updateMicPosition();

  // ---------- Step 8: Fitness intent detection --------------------------

  INTENTS.push({
    pattern: /\b(run|ran|walk|gym|workout|workouts|train|training|session|exercise|bjj|wrestling|boxing|yoga|swim|cycle|lift|jog|sprint|hiit|cardio|squat|push.?up|pull.?up|bench|deadlift|lunge|plank)\b|\b(log|did|add|just did|completed)\s.{0,30}(workout|session|exercise|training|run|swim|yoga|gym)\b/i,
    action: function(transcript) { logFitnessViaVoice(transcript); }
  });

  async function logFitnessViaVoice(transcript) {
    speak('Logging your workout');
    if (typeof window.toast === 'function') window.toast('\ud83c\udfa4 Processing workout...', 'info');
    const token = localStorage.getItem('token');
    if (!token) { speak('Please log in first'); return; }
    let workoutName = transcript;
    let durationMinutes = null;
    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: 'Extract workout info from this voice command: "' + transcript + '". Reply ONLY with a JSON object like: {"workoutType":"Running","durationMinutes":30}. If duration not mentioned, use null.' })
      });
      if (res.ok) {
        const json = await res.json();
        const raw = json?.data?.message?.content || '';
        try {
          const match = raw.match(/\{[^}]+\}/);
          if (match) {
            const parsed = JSON.parse(match[0]);
            if (parsed.workoutType) workoutName = parsed.workoutType;
            if (parsed.durationMinutes) durationMinutes = parsed.durationMinutes;
          }
        } catch(_) {}
      }
    } catch(_) {}
    try {
      await window.apiPost('/api/workouts', { name: workoutName, status: 'PENDING' });
      if (typeof window.loadActiveWorkouts === 'function') window.loadActiveWorkouts();
      const confirmation = '\u2705 Logged: ' + workoutName + (durationMinutes ? ' \u2014 ' + durationMinutes + ' min' : '');
      speak('Logged ' + workoutName);
      if (typeof window.toast === 'function') window.toast(confirmation, 'success');
      // Goal nudge
      setTimeout(async function() {
        try {
          const gr = await window.apiGet('/api/goals');
          const active = (gr?.data || []).filter(function(g) { return g.status === 'ACTIVE'; });
          const fitnessGoal = active.find(function(g) {
            return g.category === 'FITNESS' || /workout|training|fitness|exercise/i.test(g.title || '');
          });
          if (fitnessGoal && typeof window.toast === 'function') {
            window.toast('\ud83d\udcaa Great session! Keep going for: ' + fitnessGoal.title, 'info');
          }
        } catch(_) {}
      }, 2000);
    } catch(err) {
      speak('Could not log workout');
      if (typeof window.toast === 'function') window.toast('Failed to log workout. Try again.', 'error');
    }
  }

  // Expose for testing
  window._voiceHandleCommand = handleCommand;

})();
