package com.yourapp.scheduler;

import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.model.User;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import com.yourapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Runs every hour.
     * Finds tasks due within the next 2 hours that haven't had a reminder sent yet,
     * sends a notification to the task owner via their preferred channel,
     * and marks the reminder as sent.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void sendUpcomingTaskReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(2);

        List<Task> upcomingTasks = taskRepository
                .findByStatusAndDueDateBeforeAndReminderSentFalse(TaskStatus.PENDING, cutoff);

        log.info("Reminder scheduler — found {} tasks due within 2 hours", upcomingTasks.size());

        for (Task task : upcomingTasks) {
            try {
                Optional<User> userOpt = userRepository.findById(task.getUserId());
                if (userOpt.isEmpty()) continue;

                User user = userOpt.get();
                String dueDate = task.getDueDate().format(FORMATTER);
                String message = String.format(
                        "⏰ Task Reminder: \"%s\" is due on %s. Stay focused! 💪",
                        task.getTitle(), dueDate);

                notificationService.sendNotification(user, message, "REMINDER");

                task.setReminderSent(true);
                taskRepository.save(task);

                log.info("Reminder sent for task '{}' to user {}", task.getTitle(), user.getEmail());
            } catch (Exception e) {
                log.error("Error sending reminder for task {}: {}", task.getId(), e.getMessage());
            }
        }
    }

    /**
     * Runs daily at 8 AM — sends a daily summary of pending tasks.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendDailySummary() {
        log.info("Daily summary scheduler triggered");

        List<User> users = userRepository.findAll();

        for (User user : users) {
            List<Task> pendingTasks = taskRepository
                    .findByUserIdAndStatus(user.getId(), TaskStatus.PENDING);

            if (pendingTasks.isEmpty()) continue;

            StringBuilder sb = new StringBuilder();
            sb.append("🌅 Good Morning, ").append(user.getFullName()).append("!\n\n");
            sb.append("📋 You have ").append(pendingTasks.size()).append(" pending task(s) today:\n\n");

            int limit = Math.min(pendingTasks.size(), 5);
            for (int i = 0; i < limit; i++) {
                Task t = pendingTasks.get(i);
                sb.append(i + 1).append(". ").append(t.getTitle());
                if (t.getDueDate() != null) {
                    sb.append(" — due ").append(t.getDueDate().format(FORMATTER));
                }
                sb.append("\n");
            }
            if (pendingTasks.size() > 5) {
                sb.append("... and ").append(pendingTasks.size() - 5).append(" more.\n");
            }
            sb.append("\n💪 Make today count! — AI Execution System");

            notificationService.sendNotification(user, sb.toString(), "DAILY_SUMMARY");
        }
    }

    /**
     * Called when a task's deadline has passed without completion.
     */
    public void notifyTaskTimeout(User user, String taskName) {
        notificationService.sendNotification(
                user,
                "⚠️ Task expired: \"" + taskName + "\". Consider rescheduling or marking it cancelled.",
                "TIMEOUT");
    }

    /**
     * Runs every 30 minutes. Finds HIGH/URGENT/MEDIUM priority tasks due within the next
     * 24 hours and sends a deadline-approaching reminder (only once per task).
     */
    @Scheduled(fixedRate = 1_800_000)
    public void sendDeadlineReminders() {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime in24h  = now.plusHours(24);

        List<com.yourapp.model.Task.TaskPriority> highPriorities = List.of(
                com.yourapp.model.Task.TaskPriority.HIGH,
                com.yourapp.model.Task.TaskPriority.URGENT,
                com.yourapp.model.Task.TaskPriority.MEDIUM);
        List<TaskStatus> excludeStatuses = List.of(
                TaskStatus.COMPLETED, TaskStatus.DONE, TaskStatus.CANCELLED);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                List<Task> approaching = taskRepository
                        .findByUserIdAndPriorityInAndDueDateBetweenAndStatusNotIn(
                                user.getId(), highPriorities, now, in24h, excludeStatuses);
                for (Task task : approaching) {
                    if (task.isReminderSent()) continue;
                    String due = task.getDueDate().format(FORMATTER);
                    String msg = String.format(
                            "⚡ Deadline in 24h: \"%s\" (%s priority) — due %s",
                            task.getTitle(), task.getPriority(), due);
                    notificationService.sendNotification(user, msg, "DEADLINE");
                    task.setReminderSent(true);
                    taskRepository.save(task);
                }
            } catch (Exception e) {
                log.error("Deadline reminder error for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
