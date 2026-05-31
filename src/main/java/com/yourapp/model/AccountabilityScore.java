package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "accountability_scores")
@CompoundIndex(name = "user_date_unique", def = "{'userId': 1, 'date': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountabilityScore {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate date;

    /** 0–100 total score */
    private int score;

    /**
     * Breakdown: taskPoints, streakPoints, sessionPoints,
     * habitPoints, moodPoints, reflectionPoints
     */
    private Map<String, Integer> breakdown;

    /** One-line AI comment from Groq */
    private String aiComment;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
