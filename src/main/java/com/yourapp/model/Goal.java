package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    private String description;

    private GoalCategory category;

    private LocalDate targetDate;

    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    @Builder.Default
    private List<String> milestones = new ArrayList<>();

    private String aiInsight;     // AI-generated analysis/advice

    private int progressPercent; // 0 – 100

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /** User IDs of collaborators who can view/contribute to this goal (nullable). */
    @Builder.Default
    private List<String> collaboratorIds = new ArrayList<>();

    /** Optional life area tag — references a LifeArea document id. */
    private String lifeAreaId;

    public enum GoalCategory {
        CAREER, FINANCE, FITNESS, EDUCATION, PERSONAL, BUSINESS, HEALTH, RELATIONSHIP, OTHER
    }

    public enum GoalStatus {
        ACTIVE, PAUSED, COMPLETED, ABANDONED
    }
}
