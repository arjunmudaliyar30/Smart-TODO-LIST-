package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "evening_reflections")
@CompoundIndex(name = "user_date_unique", def = "{'userId': 1, 'date': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EveningReflection {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate date;

    /** Q1: What did you accomplish today? */
    private String q1Answer;

    /** Q2: What blocked you? */
    private String q2Answer;

    /** Q3: What is your #1 priority for tomorrow? */
    private String q3Answer;

    /** AI-generated 3–5 sentence summary */
    private String aiSummary;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
