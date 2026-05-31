package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "mood_logs")
@CompoundIndex(name = "user_date_unique", def = "{'userId': 1, 'date': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private LocalDate date;

    /** 1–5 energy rating */
    private int energy;

    /** 1–5 mood rating */
    private int mood;

    /** 1–5 focus rating */
    private int focus;

    /** Optional voice note transcription */
    private String voiceNote;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
