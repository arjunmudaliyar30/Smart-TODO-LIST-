package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "life_areas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifeArea {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** e.g. Health, Career, Learning, Finance, Relationships, Spirituality */
    private String name;

    /** Hex color for UI e.g. #4caf50 */
    private String color;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
