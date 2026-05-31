package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "milestones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Milestone {

    @Id
    private String id;

    @Indexed
    private String userId;

    /**
     * e.g. JOINED, FIRST_TASK, STREAK_7, STREAK_30, TASKS_100,
     *      FIRST_WEEKLY_REVIEW, FIRST_REFLECTION, PHOTOS_30,
     *      BRAIN_STREAK_7, SCORE_90
     */
    private String type;

    /** Human-readable label e.g. "7 Day Streak 🔥" */
    private String label;

    @Indexed
    private LocalDate date;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
