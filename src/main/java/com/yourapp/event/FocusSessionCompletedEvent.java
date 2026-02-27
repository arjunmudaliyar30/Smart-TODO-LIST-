package com.yourapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class FocusSessionCompletedEvent extends ApplicationEvent {
    private final String userId;
    private final String sessionId;
    private final String linkedTaskId;
    private final LocalDate sessionDate;

    public FocusSessionCompletedEvent(Object source, String userId, String sessionId,
                                      String linkedTaskId, LocalDate sessionDate) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
        this.linkedTaskId = linkedTaskId;
        this.sessionDate = sessionDate;
    }
}
