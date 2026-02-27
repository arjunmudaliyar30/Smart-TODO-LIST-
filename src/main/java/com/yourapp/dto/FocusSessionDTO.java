package com.yourapp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionDTO {
    private String id;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;
    private String linkedTaskId;
    private boolean completed;
    private boolean expired;
}
