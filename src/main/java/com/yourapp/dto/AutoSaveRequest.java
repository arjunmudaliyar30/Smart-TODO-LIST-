package com.yourapp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AutoSaveRequest {
    /** Either noteId OR date must be populated. */
    private String    noteId;
    private LocalDate date;
    private String    content; // nullable
}
