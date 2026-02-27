package com.yourapp.scheduler;

import com.yourapp.model.Task;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import com.yourapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fires daily reminder notifications for tasks marked as recurring.
 *
 * <p>Runs every minute. When the current time (HH:mm) matches a task's
 * {@code recurringTime} and the notification has not already been sent
 * today, it:
 * <ol>
 *   <li>Notifies the task owner.</li>
 *   <li>Notifies all collaborators.</li>
 *   <li>Resets the task status to PENDING (new day, fresh start).</li>
 *   <li>Updates {@code recurringNotifiedDate} to today to prevent duplicates.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTaskScheduler {

    private final TaskRepository       taskRepository;
    private final UserRepository       userRepository;
    private final NotificationService  notificationService;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(cron = "0 * * * * *")   // every minute at :00 seconds
    public void fireRecurringTaskReminders() {
        String currentTime = LocalTime.now().format(HH_MM);
        LocalDate today     = LocalDate.now();

        List<Task> tasks = taskRepository.findByRecurringTrue();
        for (Task task : tasks) {
            if (task.getRecurringTime() == null || !task.getRecurringTime().equals(currentTime)) continue;
            if (today.equals(task.getRecurringNotifiedDate())) continue; // already notified today

            // --- Notify owner ---
            userRepository.findById(task.getUserId()).ifPresent(owner ->
                    notificationService.sendNotification(owner,
                            "\u23F0 Daily Reminder: " + task.getTitle(),
                            "DAILY_TASK"));

            // --- Notify each collaborator ---
            if (task.getCollaboratorIds() != null) {
                for (String collabId : task.getCollaboratorIds()) {
                    userRepository.findById(collabId).ifPresent(collab ->
                            notificationService.sendNotification(collab,
                                    "\u23F0 Daily Task: " + task.getTitle(),
                                    "DAILY_TASK"));
                }
            }

            // --- Reset for the new day ---
            task.setStatus(Task.TaskStatus.PENDING);
            task.setCompletedAt(null);
            task.setStartTime(null);
            task.setRecurringNotifiedDate(today);
            taskRepository.save(task);

            log.info("DAILY_TASK taskId={} title={} notified at {}",
                    task.getId(), task.getTitle(), currentTime);
        }
    }
}
