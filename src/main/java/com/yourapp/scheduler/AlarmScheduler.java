package com.yourapp.scheduler;

import com.yourapp.model.Task;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import com.yourapp.service.AlarmService;
import com.yourapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fires one-time task alarms.
 *
 * <p>Runs every minute. For each task where:
 * <ul>
 *   <li>{@code alarmTime} is set and has passed</li>
 *   <li>{@code alarmFired} is false</li>
 * </ul>
 * it sends an in-app notification + email to the task owner, then marks
 * {@code alarmFired = true} to prevent duplicate alerts.
 * The email is delivered server-side, so it arrives even when the browser is closed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AlarmScheduler {

    private final TaskRepository      taskRepository;
    private final UserRepository      userRepository;
    private final NotificationService notificationService;
    private final AlarmService        alarmService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")   // every minute at :00 seconds
    public void fireAlarms() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> pending = taskRepository.findPendingAlarms(now);

        for (Task task : pending) {
            userRepository.findById(task.getUserId()).ifPresent(owner -> {
                // In-app + Web Push notification (push fires even when browser is closed)
                notificationService.sendNotification(
                        owner,
                        "\u23F0 Alarm: " + task.getTitle()
                                + (task.getDescription() != null && !task.getDescription().isBlank()
                                        ? " — " + task.getDescription() : ""),
                        "ALARM");
            });

            // Also create a standalone Alarm so alarm-overlay.js shows the fullscreen overlay
            alarmService.create(task.getUserId(), "⏰ " + task.getTitle(), task.getAlarmTime());

            // Mark fired so the alarm doesn't repeat
            task.setAlarmFired(true);
            taskRepository.save(task);
            log.info("ALARM_FIRED taskId={} title={} alarmTime={}",
                    task.getId(), task.getTitle(), task.getAlarmTime());
        }
    }
}
