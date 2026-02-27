package com.yourapp.event;

import com.yourapp.model.Task;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class TaskCompletedEvent extends ApplicationEvent {
    private final String userId;
    private final String taskId;
    private final LocalDate completedDate;

    public TaskCompletedEvent(Object source, String userId, String taskId, LocalDate completedDate) {
        super(source);
        this.userId = userId;
        this.taskId = taskId;
        this.completedDate = completedDate;
    }
}
