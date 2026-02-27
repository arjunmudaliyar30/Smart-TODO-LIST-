package com.yourapp.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {
    private String id;
    private String achievementId;
    private String name;
    private String description;
    private String badgeIcon;
    private LocalDate achievedDate;
}
