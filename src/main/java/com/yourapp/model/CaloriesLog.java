package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "calories_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaloriesLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private LocalDate date;

    /** Calories consumed (food intake) */
    private int consumed;

    /** Calories burned (exercise, etc.) */
    private int burned;

    /** BREAKFAST, LUNCH, DINNER, SNACK, WORKOUT, OTHER */
    private String mealType;

    private String note;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
