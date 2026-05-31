package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "one_things")
@CompoundIndex(name = "user_date_unique", def = "{'userId': 1, 'date': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneThing {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate date;

    /** The single most important task text for today */
    private String taskText;

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
