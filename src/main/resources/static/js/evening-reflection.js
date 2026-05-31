/* ========= Evening Reflection JS ========= */
(function () {
    'use strict';

    function renderEveningReflection(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = `
        <div class="evening-reflection-card">
            <h3>🌙 Evening Reflection</h3>
            <div class="reflection-question">
                <label>What was your biggest highlight today?</label>
                <textarea id="reflQ1" placeholder="Something you're proud of or grateful for..."></textarea>
            </div>
            <div class="reflection-question">
                <label>What was your biggest challenge?</label>
                <textarea id="reflQ2" placeholder="Something difficult or unexpected..."></textarea>
            </div>
            <div class="reflection-question">
                <label>What's your #1 focus for tomorrow?</label>
                <textarea id="reflQ3" placeholder="One key intention for tomorrow..."></textarea>
            </div>
            <button class="btn-save-reflection" id="btnSaveReflection">Save Reflection</button>
            <div id="reflDoneBadge" class="reflection-done-badge" style="display:none">
                ✅ Reflection saved for today
            </div>
            <div id="reflAiSummary" style="display:none">
                <div class="reflection-ai-label">✨ AI Summary</div>
                <div class="reflection-ai-summary" id="reflAiText"></div>
            </div>
        </div>`;

        document.getElementById('btnSaveReflection').addEventListener('click', saveReflection);

        // Load today's existing reflection if any
        loadTodayReflection();
    }

    async function loadTodayReflection() {
        try {
            const res = await apiGet('/api/reflection/history');
            if (res && res.data && res.data.length > 0) {
                const todayStr = new Date().toISOString().split('T')[0];
                const todayEntry = res.data.find(r => r.date === todayStr);
                if (todayEntry) {
                    setTextareas(todayEntry.q1Answer, todayEntry.q2Answer, todayEntry.q3Answer);
                    showDoneBadge();
                    if (todayEntry.aiSummary) showAiSummary(todayEntry.aiSummary);
                }
            }
        } catch (_) { /* ok */ }
    }

    function setTextareas(q1, q2, q3) {
        const t1 = document.getElementById('reflQ1');
        const t2 = document.getElementById('reflQ2');
        const t3 = document.getElementById('reflQ3');
        if (t1) t1.value = q1 || '';
        if (t2) t2.value = q2 || '';
        if (t3) t3.value = q3 || '';
    }

    async function saveReflection() {
        const q1 = (document.getElementById('reflQ1').value || '').trim();
        const q2 = (document.getElementById('reflQ2').value || '').trim();
        const q3 = (document.getElementById('reflQ3').value || '').trim();

        if (!q1 && !q2 && !q3) return;

        const btn = document.getElementById('btnSaveReflection');
        if (btn) { btn.textContent = 'Saving...'; btn.disabled = true; }

        try {
            const res = await apiPost('/api/reflection', { q1Answer: q1, q2Answer: q2, q3Answer: q3 });
            showDoneBadge();
            const aiSummary = res && res.data && res.data.aiSummary ? res.data.aiSummary : null;
            if (aiSummary) {
                showAiSummary(aiSummary);
            }

            // Auto-save reflection to today's daily note with AI summary appended
            try {
                const today = new Date().toISOString().split('T')[0];
                const noteRes = await apiGet('/api/notes?date=' + today);
                const existingContent = (noteRes && noteRes.data && noteRes.data.content) ? noteRes.data.content : '';
                const reflectionBlock = [
                    '',
                    '--- 🌙 Evening Reflection ---',
                    q1 ? '✨ Highlight: ' + q1 : '',
                    q2 ? '💪 Challenge: ' + q2 : '',
                    q3 ? '🎯 Tomorrow: ' + q3 : '',
                    aiSummary ? '\n🤖 AI Summary: ' + aiSummary : ''
                ].filter(Boolean).join('\n');
                const mergedContent = existingContent ? existingContent + '\n' + reflectionBlock : reflectionBlock.trim();
                await apiPost('/api/notes', {
                    date: today,
                    title: noteRes && noteRes.data && noteRes.data.title ? noteRes.data.title : '🌙 Reflection – ' + new Date().toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }),
                    content: mergedContent
                });
                // Refresh notes grid if visible
                if (typeof loadNoteCards === 'function') loadNoteCards();
            } catch (_e) { console.warn('Reflection note save failed', _e); }

            // Trigger recalculate
            apiPost('/api/score/today/recalculate', {}).catch(() => {});
        } catch (err) {
            console.error('Failed to save reflection', err);
        } finally {
            if (btn) { btn.textContent = 'Save Reflection'; btn.disabled = false; }
        }
    }

    function showDoneBadge() {
        const badge = document.getElementById('reflDoneBadge');
        if (badge) badge.style.display = 'flex';
    }

    function showAiSummary(text) {
        const wrapper = document.getElementById('reflAiSummary');
        const textEl  = document.getElementById('reflAiText');
        if (wrapper) wrapper.style.display = 'block';
        if (textEl)  textEl.textContent = text;
    }

    window.EveningReflection = { render: renderEveningReflection };
})();
