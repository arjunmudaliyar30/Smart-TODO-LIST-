/* ========= Mood Logger JS ========= */
(function () {
    'use strict';

    const EMOJIS = { 1: '😞', 2: '😕', 3: '😐', 4: '😊', 5: '😄' };

    function renderMoodLogger(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = `
        <div class="mood-logger-card">
            <h3>⚡ Daily Energy Check-In</h3>
            <div class="mood-sliders">
                ${buildSlider('energy', 'Energy')}
                ${buildSlider('mood', 'Mood')}
                ${buildSlider('focus', 'Focus')}
            </div>
            <button class="btn-log-mood" id="btnLogMood">Save Check-In</button>
            <div id="moodLoggedBadge" class="mood-logged-badge" style="display:none">
                ✅ Logged for today
            </div>
        </div>`;

        // Live emoji update
        ['energy', 'mood', 'focus'].forEach(key => {
            const slider = document.getElementById('slider_' + key);
            const valEl  = document.getElementById('val_' + key);
            const emoji  = document.getElementById('emoji_' + key);
            slider.addEventListener('input', () => {
                valEl.textContent  = slider.value;
                emoji.textContent  = EMOJIS[slider.value] || '😐';
            });
        });

        document.getElementById('btnLogMood').addEventListener('click', saveMood);

        // Load today's log
        loadTodayMood();
    }

    function buildSlider(key, label) {
        return `
        <div class="mood-slider-row">
            <span class="mood-slider-label">${label}</span>
            <span class="mood-emoji" id="emoji_${key}">😐</span>
            <input type="range" id="slider_${key}" min="1" max="5" value="3">
            <span class="mood-slider-value" id="val_${key}">3</span>
        </div>`;
    }

    async function loadTodayMood() {
        try {
            const res = await apiGet('/api/mood/today');
            if (res && res.data) {
                setSliders(res.data.energy, res.data.mood, res.data.focus);
                showLoggedBadge();
            }
        } catch (_) { /* first time, no log yet */ }
    }

    function setSliders(energy, mood, focus) {
        const vals = { energy, mood, focus };
        ['energy', 'mood', 'focus'].forEach(k => {
            const v = vals[k] || 3;
            const slider = document.getElementById('slider_' + k);
            const valEl  = document.getElementById('val_' + k);
            const emoji  = document.getElementById('emoji_' + k);
            if (slider) {
                slider.value       = v;
                valEl.textContent  = v;
                emoji.textContent  = EMOJIS[v] || '😐';
            }
        });
    }

    async function saveMood() {
        const body = {
            energy: parseInt(document.getElementById('slider_energy').value),
            mood:   parseInt(document.getElementById('slider_mood').value),
            focus:  parseInt(document.getElementById('slider_focus').value)
        };
        try {
            await apiPost('/api/mood', body);
            showLoggedBadge();
            // Trigger score recalculation
            apiPost('/api/score/today/recalculate', {}).catch(() => {});
        } catch (err) {
            console.error('Failed to save mood', err);
        }
    }

    function showLoggedBadge() {
        const badge = document.getElementById('moodLoggedBadge');
        if (badge) badge.style.display = 'flex';
    }

    // Expose
    window.MoodLogger = { render: renderMoodLogger };
})();
