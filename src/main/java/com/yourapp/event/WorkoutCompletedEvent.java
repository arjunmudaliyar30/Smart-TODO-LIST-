package com.yourapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class WorkoutCompletedEvent extends ApplicationEvent {
    private final String userId;
    private final String workoutId;
    private final LocalDate completedDate;

    public WorkoutCompletedEvent(Object source, String userId, String workoutId, LocalDate completedDate) {
        super(source);
        this.userId = userId;
        this.workoutId = workoutId;
        this.completedDate = completedDate;
    }
}
