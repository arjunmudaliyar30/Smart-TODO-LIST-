package com.yourapp.dto;

import com.yourapp.model.Goal.GoalCategory;
import com.yourapp.model.Goal.GoalStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private GoalCategory category;

    private LocalDate targetDate;

    private GoalStatus status;

    private List<String> milestones;

    private int progressPercent;
}
