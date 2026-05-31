package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String type;          // e.g. "Gym", "Run", "Yoga", "Cycling"
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private String notes;
    private String mood;          // e.g. "Energized", "Tired", "Neutral"

    @Builder.Default
    private LocalDate sessionDate = LocalDate.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
