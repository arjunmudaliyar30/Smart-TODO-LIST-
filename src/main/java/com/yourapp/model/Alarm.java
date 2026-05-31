package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alarms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alarm {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    @Indexed
    private LocalDateTime scheduledAt;

    @Builder.Default
    private boolean fired = false;

    @Builder.Default
    private boolean dismissed = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
