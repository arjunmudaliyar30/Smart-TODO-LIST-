package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "file_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String originalFilename;

    private String storedFilename;

    private String filePath;

    private String contentType;

    private long fileSize;

    private String taskId;        // optional — link file to a task

    private String description;

    /** User IDs of collaborators who can access this file (nullable). */
    @Builder.Default
    private java.util.List<String> collaboratorIds = new java.util.ArrayList<>();

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
