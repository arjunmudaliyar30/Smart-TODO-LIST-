package com.yourapp.scheduler;

import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import com.yourapp.service.GoalService;
import com.yourapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every 60 seconds: find in-progress tasks that have autoComplete=true
 * and whose startTime + durationMinutes has elapsed, then mark them DONE
 * and trigger notifications + goal updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AutoCompleteScheduler {

    private final TaskRepository     taskRepository;
    private final UserRepository     userRepository;
    private final NotificationService notificationService;
    private final GoalService        goalService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void completeExpiredDurationTasks() {
        LocalDateTime now = LocalDateTime.now();

        List<Task> candidates = taskRepository
                .findByAutoCompleteTrueAndStatusNotInAndStartTimeNotNull(
                        List.of(TaskStatus.DONE, TaskStatus.COMPLETED, TaskStatus.CANCELLED));

        if (candidates.isEmpty()) return;

        log.debug("AutoComplete: checking {} candidate task(s)", candidates.size());

        for (Task task : candidates) {
            try {
                if (task.getStartTime() == null || task.getDurationMinutes() == null) continue;

                LocalDateTime deadline = task.getStartTime().plusMinutes(task.getDurationMinutes());
                if (deadline.isAfter(now)) continue; // not expired yet

                task.setStatus(TaskStatus.DONE);
                task.setCompletedAt(now);
                task.setUpdatedAt(now);
                taskRepository.save(task);

                // Notify task owner
                userRepository.findById(task.getUserId()).ifPresent(user ->
                        notificationService.sendNotification(user,
                                "✅ Task \"" + task.getTitle() + "\" was auto-completed after "
                                        + task.getDurationMinutes() + " min.",
                                "AUTO_COMPLETE"));

                // Notify collaborators
                if (task.getCollaboratorIds() != null) {
                    for (String collabId : task.getCollaboratorIds()) {
                        if (collabId == null) continue;
                        userRepository.findById(collabId).ifPresent(collab ->
                                notificationService.sendNotification(collab,
                                        "ℹ️ Shared task \"" + task.getTitle() + "\" was auto-completed.",
                                        "AUTO_COMPLETE"));
                    }
                }

                // Update linked goal progress
                if (task.getGoalId() != null) {
                    try {
                        goalService.recalculateGoalProgress(task.getGoalId());
                    } catch (Exception ex) {
                        log.warn("AutoComplete goal recalc failed goalId={}: {}", task.getGoalId(), ex.getMessage());
                    }
                }

                log.info("AutoCompleted task id={} title='{}' userId={}",
                        task.getId(), task.getTitle(), task.getUserId());

            } catch (Exception ex) {
                log.error("AutoComplete error for task id={}: {}", task.getId(), ex.getMessage(), ex);
            }
        }
    }
}
