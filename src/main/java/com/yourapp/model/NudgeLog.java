package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "nudge_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NudgeLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate date;

    private String nudgeText;

    /** e.g. STREAK_RISK, HABIT_SKIP, NO_SESSION, LOW_MOOD, HIGH_SCORE, ONE_THING_IDLE */
    private String trigger;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
