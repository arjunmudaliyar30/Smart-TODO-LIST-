package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String message;

    private String type;

    @Builder.Default
    private boolean read = false;

    private NotificationChannel channelUsed;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
