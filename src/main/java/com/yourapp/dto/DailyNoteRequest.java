package com.yourapp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyNoteRequest {
    private LocalDate date;     // required for create; optional for update
    private String    title;
    private String    content;  // nullable — auto-saved empty is OK
    private String    color;
    private Boolean   pinned;
    private String    mood;
    /** Phase 7: 1–10 energy level for mood analytics */
    private Integer   energyLevel;
    /** Phase 7: hours slept the previous night */
    private Double    sleepHours;
}
