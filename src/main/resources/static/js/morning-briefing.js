/* ========= Morning Briefing JS ========= */
(function () {
    'use strict';

    const today = new Date().toISOString().split('T')[0];
    const STORAGE_KEY = 'briefing_shown_' + today;

    /**
     * Show the morning briefing overlay if:
     *  - Current time is between 05:00 and 10:59
     *  - It hasn't been shown today yet (localStorage flag)
     */
    function maybeShowBriefing() {
        const hour = new Date().getHours();
        if (hour < 5 || hour >= 11) return;
        if (localStorage.getItem(STORAGE_KEY)) return;

        apiGet('/api/briefing/today')
            .then(res => {
                if (res && res.data && res.data.content) {
                    showOverlay(res.data.content);
                }
            })
            .catch(() => { /* user not logged in yet */ });
    }

    function showOverlay(content) {
        const greeting = getGreeting();
        const dateStr  = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });

        const overlay = document.createElement('div');
        overlay.className = 'morning-briefing-overlay';
        overlay.id = 'morningBriefingOverlay';
        overlay.innerHTML = `
            <div class="morning-briefing-card">
                <div class="morning-briefing-greeting">${greeting}</div>
                <div class="morning-briefing-date">${dateStr}</div>
                <div class="morning-briefing-content">${escapeHtml(content)}</div>
                <div class="morning-briefing-actions">
                    <button class="btn-briefing-start" id="btnBriefingStart">Let's Go 🚀</button>
                    <button class="btn-briefing-dismiss" id="btnBriefingDismiss">Dismiss</button>
                </div>
            </div>`;

        document.body.appendChild(overlay);

        document.getElementById('btnBriefingStart').addEventListener('click', dismiss);
        document.getElementById('btnBriefingDismiss').addEventListener('click', dismiss);
    }

    function dismiss() {
        localStorage.setItem(STORAGE_KEY, '1');
        const overlay = document.getElementById('morningBriefingOverlay');
        if (overlay) overlay.remove();
    }

    function getGreeting() {
        const hour = new Date().getHours();
        if (hour < 9)  return 'Good Morning ☀️';
        if (hour < 12) return 'Morning! ☀️';
        return 'Good Day! 👋';
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g,'<br>');
    }

    // Auto-run when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', maybeShowBriefing);
    } else {
        maybeShowBriefing();
    }

    window.MorningBriefing = { show: maybeShowBriefing };
})();
