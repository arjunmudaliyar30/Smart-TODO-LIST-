package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Tracks which users a DailyNote has been shared with and what they can do.
 * Unique per (noteId + sharedWithUserId) — prevents duplicate shares.
 */
@Document(collection = "daily_note_shares")
@CompoundIndex(name = "note_user_unique", def = "{'noteId': 1, 'sharedWithUserId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNoteShare {

    @Id
    private String id;

    private String noteId;

    /** UserId of the note owner (convenience for queries). */
    private String ownerId;

    private String sharedWithUserId;

    private PermissionType permissionType;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
