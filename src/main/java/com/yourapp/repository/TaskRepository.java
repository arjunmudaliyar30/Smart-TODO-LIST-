package com.yourapp.repository;

import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Task> findByUserIdAndStatus(String userId, TaskStatus status);

    List<Task> findByUserIdAndCategory(String userId, String category);

    List<Task> findByUserIdAndSection(String userId, String section);

    // Goal-linked tasks
    List<Task> findByGoalId(String goalId);

    List<Task> findByGoalIdAndStatus(String goalId, TaskStatus status);

    long countByGoalId(String goalId);

    long countByGoalIdAndStatusIn(String goalId, List<TaskStatus> statuses);

    // Collaborator queries
    List<Task> findByCollaboratorIdsContaining(String userId);

    // Tasks due soon and reminder not yet sent
    List<Task> findByStatusAndDueDateBeforeAndReminderSentFalse(
            TaskStatus status, LocalDateTime cutoff);

    // HIGH/MEDIUM priority tasks due within a window (for deadline reminders)
    List<Task> findByUserIdAndPriorityInAndDueDateBetweenAndStatusNotIn(
            String userId,
            List<com.yourapp.model.Task.TaskPriority> priorities,
            LocalDateTime from, LocalDateTime to,
            List<TaskStatus> excludeStatuses);

    Optional<Task> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    // Step 3 — scheduled date queries
    List<Task> findByUserIdAndScheduledDate(String userId, LocalDate scheduledDate);

    // Step 5 — calorie queries: tasks with calorie data on a given scheduled date
    List<Task> findByUserIdAndScheduledDateAndCaloriesConsumedNotNull(String userId, LocalDate date);

    List<Task> findByUserIdAndScheduledDateAndCaloriesBurnedNotNull(String userId, LocalDate date);

    // Phase 12 — nested task hierarchy
    List<Task> findByParentTaskId(String parentTaskId);

    // Phase 12 — auto-complete: tasks where autoComplete=true, startTime is set,
    // and status is NOT in the given list
    List<Task> findByAutoCompleteTrueAndStatusNotInAndStartTimeNotNull(List<TaskStatus> excludeStatuses);

    // Phase 10 — achievement counts
    long countByUserIdAndStatus(String userId, TaskStatus status);

    // Daily recurring tasks
    List<Task> findByRecurringTrue();

    List<Task> findByUserIdAndRecurringTrue(String userId);

    // Alarm: tasks whose alarm time has passed and alarm not yet fired
    @org.springframework.data.mongodb.repository.Query(
        "{ 'alarmFired': { $ne: true }, 'alarmTime': { $exists: true, $ne: null, $lte: ?0 } }")
    List<Task> findPendingAlarms(LocalDateTime now);
}
