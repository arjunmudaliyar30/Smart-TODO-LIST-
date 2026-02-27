package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String sessionId;

    private MessageRole role;   // USER or ASSISTANT

    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}
