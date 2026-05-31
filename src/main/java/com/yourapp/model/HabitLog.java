package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "habit_logs")
@CompoundIndex(name = "habit_user_date", def = "{'userId': 1, 'habitId': 1, 'date': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String habitId;

    @Indexed
    private LocalDate date;

    private boolean completed;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
