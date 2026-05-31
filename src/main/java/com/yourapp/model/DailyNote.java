package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One diary/journal entry per user per day.
 * Supports selective sharing via DailyNoteShare.
 */
@Document(collection = "daily_notes")
@CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNote {

    @Id
    private String id;

    @Indexed
    private String userId;

    private LocalDate date;

    /** Optional note title — shown as card heading. */
    private String title;

    /** Long-form journal content. Nullable — auto-saved. */
    private String content;

    /** Card accent color (e.g. "#6c63ff", "#f59e0b", "default"). */
    @Builder.Default
    private String color = "default";

    /** Whether this note is pinned to the top of the list. */
    @Builder.Default
    private boolean pinned = false;

    /** Free-text mood label: HAPPY, GOOD, NEUTRAL, TIRED, STRESSED, etc. */
    private String mood;

    // Phase 7: Energy & Mood Analytics
    /** Subjective energy level 1–10. Nullable. */
    private Integer energyLevel;

    /** Hours of sleep the previous night. Nullable. */
    private Double sleepHours;

    /** Soft delete — never physically removed. */
    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
