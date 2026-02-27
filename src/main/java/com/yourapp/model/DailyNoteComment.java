package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Placeholder entity — comment system reserved for future use.
 * Not fully implemented in this version.
 */
@Document(collection = "daily_note_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNoteComment {

    @Id
    private String id;

    private String noteId;

    private String authorId;

    private String text;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
