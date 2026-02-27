package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Records a single deep-work focus session.
 * When completed, marks the linked task DONE and fires a
 * FocusSessionCompletedEvent to update streaks + score.
 */
@Document(collection = "focus_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** Duration the user requested in minutes. */
    private int durationMinutes;

    /** Optional: complete this task when session ends. */
    private String linkedTaskId;

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    private boolean expired = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
