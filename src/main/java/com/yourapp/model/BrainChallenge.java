package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A daily cognitive challenge issued to the user by the AI Brain Coach.
 */
@Document(collection = "brain_challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrainChallenge {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String question;
    private String hint;
    private String answer;       // shown after user submits
    private String category;     // e.g. "Logic", "Memory", "Math", "Lateral Thinking"
    private String difficulty;   // "Easy", "Medium", "Hard"

    private String userAnswer;
    private Boolean correct;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime answeredAt;
}
