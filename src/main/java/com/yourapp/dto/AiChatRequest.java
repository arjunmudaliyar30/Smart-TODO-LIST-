package com.yourapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String sessionId;   // optional — groups messages into a conversation
}
