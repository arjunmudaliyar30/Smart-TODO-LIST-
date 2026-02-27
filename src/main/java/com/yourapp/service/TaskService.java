package com.yourapp.service;

import com.yourapp.dto.DailyCaloriesDTO;
import com.yourapp.dto.TaskRequest;
import com.yourapp.event.TaskCompletedEvent;
import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository         taskRepository;
    private final GoalService             goalService;
    private final NotificationService     notificationService;
    private final UserRepository          userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Task createTask(String userId, TaskRequest request) {
        // Validation
        if (Boolean.TRUE.equals(request.getAutoComplete()) && request.getDurationMinutes() == null) {
            throw new IllegalArgumentException("autoComplete requires durationMinutes to be set");
        }
        if (request.getDurationMinutes() != null && request.getDurationMinutes() < 1) {
            throw new IllegalArgumentException("durationMinutes must be >= 1");
        }

        List<String> collabIds = sanitizeCollaborators(request.getCollaboratorIds());

        Task task = Task.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .category(request.getCategory())
                .section(request.getSection())
                .goalId(request.getGoalId())
                .shared(!collabIds.isEmpty() || request.isShared())
                .collaboratorIds(collabIds)
                .scheduledDate(request.getScheduledDate())
                .fitnessType(request.getFitnessType())
                .caloriesConsumed(request.getCaloriesConsumed())
                .caloriesBurned(request.getCaloriesBurned())
                .fitnessCategoryId(request.getFitnessCategoryId())
                .parentTaskId(request.getParentTaskId())
                .durationMinutes(request.getDurationMinutes())
                .autoComplete(request.getAutoComplete())
                .collaborativeEditable(request.isCollaborativeEditable())
                .recurring(request.isRecurring())
                .recurringTime(request.getRecurringTime())
                .build();

        // Auto-set startTime if created directly as IN_PROGRESS
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            task.setStartTime(LocalDateTime.now());
        }

        return taskRepository.save(task);
    }

    public List<Task> getUserTasks(String userId) {
        List<Task> owned  = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Task> shared = taskRepository.findByCollaboratorIdsContaining(userId);
        // Merge, deduplicating by ID
        Set<String> seen   = new HashSet<>();
        List<Task>  merged = new ArrayList<>(owned);
        owned.forEach(t -> seen.add(t.getId()));
        shared.forEach(t -> { if (seen.add(t.getId())) merged.add(t); });
        return merged;
    }

    public List<Task> getTasksByStatus(String userId, TaskStatus status) {
        return taskRepository.findByUserIdAndStatus(userId, status);
    }

    /** Step 3: return tasks scheduled on a specific date; empty list if none */
    public List<Task> getTasksByDate(String userId, LocalDate date) {
        if (date == null) return List.of();
        List<Task> tasks = taskRepository.findByUserIdAndScheduledDate(userId, date);
        return tasks != null ? tasks : List.of();
    }

    /** Step 5 legacy — returns only net calories. Delegates to calculateDailyCalories. */
    public int calculateDailyNetCalories(String userId, LocalDate date) {
        return calculateDailyCalories(userId, date).getNet();
    }

    public Task getTaskById(String userId, String taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    public Task updateTask(String userId, String taskId, TaskRequest request) {
        Task task = getTaskById(userId, taskId);

        if (request.getTitle()       != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            TaskStatus prev = task.getStatus();
            task.setStatus(request.getStatus());
            boolean isDone = request.getStatus() == TaskStatus.COMPLETED
                    || request.getStatus() == TaskStatus.DONE;
            if (isDone && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
            // Track when task actually starts
            if (request.getStatus() == TaskStatus.IN_PROGRESS
                    && prev != TaskStatus.IN_PROGRESS
                    && task.getStartTime() == null) {
                task.setStartTime(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate()  != null) task.setDueDate(request.getDueDate());
        if (request.getCategory() != null) task.setCategory(request.getCategory());
        if (request.getSection()  != null) task.setSection(request.getSection());
        if (request.getGoalId()   != null) task.setGoalId(request.getGoalId());
        if (request.getCollaboratorIds() != null) {
            List<String> cleaned = sanitizeCollaborators(request.getCollaboratorIds());
            task.setCollaboratorIds(cleaned);
            task.setShared(!cleaned.isEmpty());
        }
        // Step 3-5: optional new fields
        if (request.getScheduledDate()    != null) task.setScheduledDate(request.getScheduledDate());
        if (request.getFitnessType()      != null) task.setFitnessType(request.getFitnessType());
        if (request.getCaloriesConsumed() != null) task.setCaloriesConsumed(request.getCaloriesConsumed());
        if (request.getCaloriesBurned()   != null) task.setCaloriesBurned(request.getCaloriesBurned());
        // Phase 12: new fields
        if (request.getFitnessCategoryId() != null) task.setFitnessCategoryId(request.getFitnessCategoryId());
        if (request.getParentTaskId()      != null) task.setParentTaskId(request.getParentTaskId());
        if (request.getDurationMinutes()   != null) task.setDurationMinutes(request.getDurationMinutes());
        if (request.getAutoComplete()      != null) {
            if (Boolean.TRUE.equals(request.getAutoComplete())
                    && task.getDurationMinutes() == null && request.getDurationMinutes() == null) {
                throw new IllegalArgumentException("autoComplete requires durationMinutes to be set");
            }
            task.setAutoComplete(request.getAutoComplete());
        }
        task.setCollaborativeEditable(request.isCollaborativeEditable());
        // Recurring task fields
        task.setRecurring(request.isRecurring());
        if (request.getRecurringTime() != null) task.setRecurringTime(request.getRecurringTime());

        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // Notify collaborators of the update (best-effort)
        notifyCollaborators(saved, "Shared task \"" + saved.getTitle() + "\" was updated.", "TASK_UPDATE");

        // Recalculate linked goal progress when task is marked done
        boolean isDone = saved.getStatus() == TaskStatus.COMPLETED
                || saved.getStatus() == TaskStatus.DONE;
        if (isDone && saved.getGoalId() != null) {
            try {
                goalService.recalculateGoalProgress(saved.getGoalId());
            } catch (Exception ex) {
                log.warn("Goal progress recalculation failed for goalId={}: {}", saved.getGoalId(), ex.getMessage());
            }
        }
        // Fire streak/performance event when task completes
        if (isDone) {
            LocalDate date = saved.getScheduledDate() != null
                    ? saved.getScheduledDate() : LocalDate.now();
            eventPublisher.publishEvent(new TaskCompletedEvent(this, saved.getUserId(), saved.getId(), date));
        }
        return saved;
    }

    /**
     * Allows a collaborator (or owner) to record their personal completion % and note for a task.
     * Notifies the task owner when a collaborator updates their progress.
     */
    public Task updateCollaboratorProgress(String actorId, String taskId, int completionPct, String note) {
        Task task = getTaskByIdForActor(actorId, taskId);
        if (task.getCollaboratorProgress() == null)
            task.setCollaboratorProgress(new HashMap<>());
        task.getCollaboratorProgress().put(actorId,
                Task.CollaboratorProgress.builder()
                        .completionPct(Math.max(0, Math.min(100, completionPct)))
                        .note(note)
                        .updatedAt(LocalDateTime.now())
                        .build());
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);
        // Notify task owner if the actor is a collaborator (not the owner)
        if (!actorId.equals(saved.getUserId())) {
            userRepository.findById(saved.getUserId()).ifPresent(owner -> {
                userRepository.findById(actorId).ifPresent(actor ->
                    notificationService.sendNotification(owner,
                            "\uD83D\uDCCA " + actor.getFullName() + " updated their progress on \"" +
                                    saved.getTitle() + "\" — " + completionPct + "% done.",
                            "COLLAB_PROGRESS"));
            });
        }
        return saved;
    }

    /** Returns only the recurring (daily) tasks owned by a user. */
    public List<Task> getRecurringTasks(String userId) {
        return taskRepository.findByUserIdAndRecurringTrue(userId);
    }

    /**
     * Look up a task by ID, allowing access for both the owner and any collaborator.
     * Used for collaborator status updates.
     */
    private Task getTaskByIdForActor(String actorId, String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (actorId.equals(task.getUserId())) return task;
        if (task.getCollaboratorIds() != null && task.getCollaboratorIds().contains(actorId)) return task;
        throw new IllegalArgumentException("Task not found or access denied");
    }

    /**
     * Cycle (or set explicit) status for the task, allowing both owner and collaborator.
     * Cycle order: PENDING → IN_PROGRESS → DONE → PENDING
     */
    public Task cycleStatusForActor(String actorId, String taskId, TaskStatus explicitStatus) {
        Task task = getTaskByIdForActor(actorId, taskId);
        TaskStatus prev = task.getStatus();
        TaskStatus next = explicitStatus != null ? explicitStatus : switch (prev) {
            case PENDING     -> TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> TaskStatus.DONE;
            default          -> TaskStatus.PENDING;
        };
        task.setStatus(next);
        if ((next == TaskStatus.DONE || next == TaskStatus.COMPLETED) && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (next == TaskStatus.PENDING) {
            task.setCompletedAt(null);
        }
        if (next == TaskStatus.IN_PROGRESS && prev != TaskStatus.IN_PROGRESS && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);
        boolean isDone = saved.getStatus() == TaskStatus.DONE || saved.getStatus() == TaskStatus.COMPLETED;
        if (isDone && saved.getGoalId() != null) {
            try {
                goalService.recalculateGoalProgress(saved.getGoalId());
            } catch (Exception ex) {
                log.warn("Goal progress recalculation failed for goalId={}: {}", saved.getGoalId(), ex.getMessage());
            }
        }
        if (isDone) {
            LocalDate date = saved.getScheduledDate() != null ? saved.getScheduledDate() : LocalDate.now();
            eventPublisher.publishEvent(new TaskCompletedEvent(this, saved.getUserId(), saved.getId(), date));
        }
        return saved;
    }

    /** Step 2: Simple toggle — delegates to cycleStatusForActor (supports collaborators). */
    public Task toggleStatus(String userId, String taskId) {
        return cycleStatusForActor(userId, taskId, null);
    }

    public void deleteTask(String userId, String taskId) {
        getTaskById(userId, taskId);
        taskRepository.deleteByIdAndUserId(taskId, userId);
    }

    public Task addSubtasks(String userId, String taskId, List<String> subTasks) {
        Task task = getTaskById(userId, taskId);
        task.getSubTasks().addAll(subTasks);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // -----------------------------------------------------------------------
    // Calories (Phase 12: full DTO + legacy int method)
    // -----------------------------------------------------------------------

    /**
     * Returns a full DailyCaloriesDTO for the given user and date.
     * All calorie values are always >= 0.
     */
    public DailyCaloriesDTO calculateDailyCalories(String userId, LocalDate date) {
        if (date == null) return new DailyCaloriesDTO(null, 0, 0, 0);
        try {
            List<Task> all = taskRepository.findByUserIdAndScheduledDate(userId, date);
            if (all == null || all.isEmpty()) return new DailyCaloriesDTO(date, 0, 0, 0);
            int consumed = all.stream()
                    .filter(t -> t.getCaloriesConsumed() != null && t.getCaloriesConsumed() >= 0)
                    .mapToInt(Task::getCaloriesConsumed).sum();
            int burned = all.stream()
                    .filter(t -> t.getCaloriesBurned() != null && t.getCaloriesBurned() >= 0)
                    .mapToInt(Task::getCaloriesBurned).sum();
            return new DailyCaloriesDTO(date, consumed, burned, consumed - burned);
        } catch (Exception ex) {
            log.error("Calories calc error userId={} date={}: {}", userId, date, ex.getMessage(), ex);
            return new DailyCaloriesDTO(date, 0, 0, 0);
        }
    }

    /** Returns child tasks of a parent task. */
    public List<Task> getSubTasksByParent(String parentTaskId) {
        if (parentTaskId == null) return List.of();
        return taskRepository.findByParentTaskId(parentTaskId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Removes nulls, blanks, and duplicates from a collaborator ID list. */
    private List<String> sanitizeCollaborators(List<String> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Resolves a collaborator by email and adds their MongoDB ID to the task's
     * collaboratorIds. Only the task owner can invoke this.
     */
    public Task addCollaboratorByEmail(String userId, String taskId, String collaboratorEmail) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found or access denied"));
        var collaborator = userRepository.findByEmail(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with email: " + collaboratorEmail));
        String collabId = collaborator.getId();
        if (task.getCollaboratorIds() == null) task.setCollaboratorIds(new ArrayList<>());
        if (!task.getCollaboratorIds().contains(collabId)) {
            task.getCollaboratorIds().add(collabId);
            task.setShared(true);
        }
        Task saved = taskRepository.save(task);
        notifyCollaborators(saved,
                "You were added as a collaborator on task: " + saved.getTitle(), "TASK_COLLAB");
        return saved;
    }

    /** Sends a notification to every collaborator of a task (best-effort, never throws). */
    private void notifyCollaborators(Task task, String message, String type) {
        if (task.getCollaboratorIds() == null || task.getCollaboratorIds().isEmpty()) return;
        for (String collabId : task.getCollaboratorIds()) {
            if (collabId == null) continue;
            try {
                userRepository.findById(collabId).ifPresent(user ->
                        notificationService.sendNotification(user, message, type));
            } catch (Exception ex) {
                log.warn("Collaborator notification failed collabId={}: {}", collabId, ex.getMessage());
            }
        }
    }
}
