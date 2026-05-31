package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "progress_photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressPhoto {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Reference to the FileMetadata id (stored via /api/files) */
    private String fileId;

    /** Direct URL path for serving the photo (e.g. /api/files/{fileId}/preview) */
    private String previewUrl;

    private String notes;

    @Builder.Default
    private LocalDate photoDate = LocalDate.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
