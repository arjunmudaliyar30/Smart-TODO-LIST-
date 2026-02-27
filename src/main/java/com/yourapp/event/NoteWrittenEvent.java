package com.yourapp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class NoteWrittenEvent extends ApplicationEvent {
    private final String userId;
    private final String noteId;
    private final LocalDate noteDate;

    public NoteWrittenEvent(Object source, String userId, String noteId, LocalDate noteDate) {
        super(source);
        this.userId = userId;
        this.noteId = noteId;
        this.noteDate = noteDate;
    }
}
