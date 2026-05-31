/* ========= Habit Tracker JS ========= */
(function () {
    'use strict';

    let habits = [];
    let todayLogs = {};

    function renderHabitTracker(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = `
        <div class="habit-tracker-card">
            <div class="habit-tracker-header">
                <h3>🔁 Habits</h3>
                <button class="btn-add-habit" id="btnShowAddHabit">+ Add</button>
            </div>
            <div class="habit-add-form" id="habitAddForm">
                <input type="text" id="habitNameInput" placeholder="Habit name (e.g. Morning walk)">
                <button class="btn-save-habit" id="btnSaveHabit">Save Habit</button>
            </div>
            <div class="habit-list" id="habitList">
                <div class="habits-empty">Loading habits...</div>
            </div>
        </div>`;

        document.getElementById('btnShowAddHabit').addEventListener('click', () => {
            document.getElementById('habitAddForm').classList.toggle('visible');
        });

        document.getElementById('btnSaveHabit').addEventListener('click', saveHabit);

        loadHabits();
    }

    async function loadHabits() {
        try {
            const [habitsRes, logsRes] = await Promise.all([
                apiGet('/api/habits'),
                apiGet('/api/habits/logs/today')
            ]);
            habits = (habitsRes && habitsRes.data) ? habitsRes.data : [];
            const logs = (logsRes && logsRes.data) ? logsRes.data : [];
            todayLogs = {};
            logs.forEach(l => { todayLogs[l.habitId] = l.completed; });
            renderHabitList();
        } catch (err) {
            const list = document.getElementById('habitList');
            if (list) list.innerHTML = '<div class="habits-empty">Failed to load habits</div>';
        }
    }

    function renderHabitList() {
        const list = document.getElementById('habitList');
        if (!list) return;

        if (habits.length === 0) {
            list.innerHTML = '<div class="habits-empty">No habits yet. Add your first one!</div>';
            return;
        }

        list.innerHTML = habits.map(h => {
            const done = todayLogs[h.id] === true;
            return `
            <div class="habit-item ${done ? 'completed' : ''}" data-id="${h.id}">
                <div class="habit-checkbox ${done ? 'checked' : ''}" data-id="${h.id}">
                    ${done ? '✓' : ''}
                </div>
                <span class="habit-name">${escapeHtml(h.name)}</span>
                ${h.lifeArea ? `<span class="habit-streak-badge">${escapeHtml(h.lifeArea)}</span>` : ''}
            </div>`;
        }).join('');

        // Wire up checkboxes
        list.querySelectorAll('.habit-checkbox').forEach(cb => {
            cb.addEventListener('click', () => toggleHabit(cb.dataset.id));
        });
    }

    async function toggleHabit(habitId) {
        const currentlyDone = todayLogs[habitId] === true;
        const newDone = !currentlyDone;
        try {
            await apiPost(`/api/habits/${habitId}/log`, { completed: newDone });
            todayLogs[habitId] = newDone;
            renderHabitList();
            // Trigger score recalculation silently
            apiPost('/api/score/today/recalculate', {}).catch(() => {});
        } catch (err) {
            console.error('Failed to log habit', err);
        }
    }

    async function saveHabit() {
        const nameInput = document.getElementById('habitNameInput');
        const name = nameInput ? nameInput.value.trim() : '';
        if (!name) return;

        try {
            await apiPost('/api/habits', { name, targetDays: [] });
            nameInput.value = '';
            document.getElementById('habitAddForm').classList.remove('visible');
            loadHabits();
        } catch (err) {
            console.error('Failed to save habit', err);
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    window.HabitTracker = { render: renderHabitTracker, reload: loadHabits };
})();
