package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "habits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Habit {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;

    /** e.g. ["DAILY"], ["MON","TUE","WED","THU","FRI"], ["MON","WED","FRI"] */
    private List<String> targetDays;

    /** health / career / learning / finance / relationships / spirituality */
    private String lifeArea;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
