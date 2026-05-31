package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A decision journal entry written by the user.
 */
@Document(collection = "decision_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String decision;         // what decision was made
    private String context;          // why / background
    private String outcome;          // what happened (filled in later)
    private String aiReflection;     // AI-generated reflection

    @Builder.Default
    private LocalDate decisionDate = LocalDate.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
