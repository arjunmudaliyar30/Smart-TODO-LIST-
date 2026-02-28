package com.yourapp.service;

import com.yourapp.dto.GoalRequest;
import com.yourapp.model.Goal;
import com.yourapp.model.Goal.GoalCategory;
import com.yourapp.model.Goal.GoalStatus;
import com.yourapp.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class GoalService {

    private final GoalRepository goalRepository;
    // Injected lazily via setter to avoid circular dependency
    private com.yourapp.repository.TaskRepository taskRepository;
    private com.yourapp.repository.UserRepository  userRepository;
    private NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setTaskRepository(com.yourapp.repository.TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setUserRepository(com.yourapp.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Goal createGoal(String userId, GoalRequest request) {
        Goal goal = Goal.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .targetDate(request.getTargetDate())
                .status(request.getStatus() != null ? request.getStatus() : GoalStatus.ACTIVE)
                .milestones(request.getMilestones() != null ? request.getMilestones() : List.of())
                .progressPercent(request.getProgressPercent())
                .build();
        return goalRepository.save(goal);
    }

    public List<Goal> getUserGoals(String userId) {
        List<Goal> owned  = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Goal> shared = goalRepository.findByCollaboratorIdsContaining(userId);
        // Merge, dedup by ID
        java.util.Set<String>  seen   = new java.util.HashSet<>();
        java.util.List<Goal>   merged = new java.util.ArrayList<>(owned);
        owned.forEach(g  -> seen.add(g.getId()));
        shared.forEach(g -> { if (seen.add(g.getId())) merged.add(g); });
        return merged;
    }

    public List<Goal> getGoalsByStatus(String userId, GoalStatus status) {
        return goalRepository.findByUserIdAndStatus(userId, status);
    }

    public List<Goal> getGoalsByCategory(String userId, GoalCategory category) {
        return goalRepository.findByUserIdAndCategory(userId, category);
    }

    public Goal getGoalById(String userId, String goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
    }

    public Goal updateGoal(String userId, String goalId, GoalRequest request) {
        Goal goal = getGoalById(userId, goalId);

        if (request.getTitle() != null) goal.setTitle(request.getTitle());
        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        if (request.getCategory() != null) goal.setCategory(request.getCategory());
        if (request.getTargetDate() != null) goal.setTargetDate(request.getTargetDate());
        if (request.getStatus() != null) goal.setStatus(request.getStatus());
        if (request.getMilestones() != null) goal.setMilestones(request.getMilestones());
        goal.setProgressPercent(request.getProgressPercent());
        goal.setUpdatedAt(LocalDateTime.now());

        return goalRepository.save(goal);
    }

    public Goal saveAiInsight(String userId, String goalId, String insight) {
        Goal goal = getGoalById(userId, goalId);
        goal.setAiInsight(insight);
        goal.setUpdatedAt(LocalDateTime.now());
        return goalRepository.save(goal);
    }

    public void deleteGoal(String userId, String goalId) {
        getGoalById(userId, goalId);
        goalRepository.deleteByIdAndUserId(goalId, userId);
    }

    /**
     * Recalculates a goal's progressPercent based on its linked tasks.
     * DONE + COMPLETED count as finished. Returns the updated goal.
     * Triggers milestone notifications at 50%, 75%, 100%.
     */
    public Goal recalculateGoalProgress(String goalId) {
        return goalRepository.findById(goalId).map(goal -> {
            if (taskRepository == null) return goal;
            long total = taskRepository.countByGoalId(goalId);
            if (total == 0) return goal;
            long done  = taskRepository.countByGoalIdAndStatusIn(goalId,
                    java.util.List.of(
                            com.yourapp.model.Task.TaskStatus.COMPLETED,
                            com.yourapp.model.Task.TaskStatus.DONE));
            int newPercent = (int) Math.round((done * 100.0) / total);
            int oldPercent = goal.getProgressPercent();
            goal.setProgressPercent(newPercent);
            if (newPercent >= 100 && goal.getStatus() == GoalStatus.ACTIVE) {
                goal.setStatus(GoalStatus.COMPLETED);
            }
            goal.setUpdatedAt(LocalDateTime.now());
            Goal saved = goalRepository.save(goal);

            // Fire milestone notifications at 50%, 75%, 100%
            if (notificationService != null && userRepository != null) {
                for (int milestone : new int[]{50, 75, 100}) {
                    if (oldPercent < milestone && newPercent >= milestone) {
                        userRepository.findById(goal.getUserId()).ifPresent(user ->
                                notificationService.sendGoalMilestoneNotification(user, goal.getTitle(), milestone));
                    }
                }
            }
            return saved;
        }).orElse(null);
    }

    /**
     * Resolves a collaborator by email and adds their MongoDB ID to the goal's
     * collaboratorIds. Only the goal owner can invoke this.
     */
    public Goal addCollaboratorByEmail(String userId, String goalId, String collaboratorEmail) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found or access denied"));
        if (userRepository == null)
            throw new IllegalStateException("UserRepository not available");
        var collaborator = userRepository.findByEmail(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with email: " + collaboratorEmail));
        String collabId = collaborator.getId();
        if (goal.getCollaboratorIds() == null)
            goal.setCollaboratorIds(new java.util.ArrayList<>());
        if (!goal.getCollaboratorIds().contains(collabId)) {
            goal.getCollaboratorIds().add(collabId);
        }
        goal.setUpdatedAt(LocalDateTime.now());
        return goalRepository.save(goal);
    }
}
