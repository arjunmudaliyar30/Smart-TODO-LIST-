package com.yourapp.dto;

import com.yourapp.model.PermissionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyNoteResponseDTO {

    private String      id;
    private LocalDate   date;
    private String      content;
    private String      mood;
    private boolean     isOwner;
    private PermissionType permissionType;  // null when owner

    // Dynamic contextual data (not stored in DB)
    private int    tasksCompletedToday;
    private int    workoutsCompletedToday;
    private int    caloriesNetToday;
}
