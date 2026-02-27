package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores the computed daily performance score for a user.
 * Unique per (userId, date).
 */
@Document(collection = "daily_performances")
@CompoundIndexes({
    @CompoundIndex(name = "user_date_unique", def = "{'userId': 1, 'date': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPerformance {

    @Id
    private String id;

    private String userId;
    private LocalDate date;

    /** Final blended score 0–100. */
    private double score;

    /** Component: task completion ratio (0–1). */
    private Double taskCompletionRatio;

    /** Component: workout completion ratio (0–1). */
    private Double workoutCompletionRatio;

    /** Component: calorie discipline (0–1). */
    private Double calorieDiscipline;

    /** Component: note written today (0 or 1). */
    private Double noteWritten;

    @Builder.Default
    private LocalDateTime recordedAt = LocalDateTime.now();
}
