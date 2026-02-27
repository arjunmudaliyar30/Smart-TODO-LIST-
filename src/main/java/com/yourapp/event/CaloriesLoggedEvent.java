package com.yourapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class CaloriesLoggedEvent extends ApplicationEvent {
    private final String userId;
    private final LocalDate logDate;
    private final double totalConsumed;
    private final double calorieGoal;

    public CaloriesLoggedEvent(Object source, String userId, LocalDate logDate,
                               double totalConsumed, double calorieGoal) {
        super(source);
        this.userId = userId;
        this.logDate = logDate;
        this.totalConsumed = totalConsumed;
        this.calorieGoal = calorieGoal;
    }
}
