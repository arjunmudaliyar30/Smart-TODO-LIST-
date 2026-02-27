package com.yourapp.dto;

import com.yourapp.model.WorkoutSchedule.DayPlan;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutScheduleRequest {
    private LocalDate    weekStartDate; // The Monday of the week
    private List<DayPlan> days;
}
