package com.yourapp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CaloriesLogRequest {

    private LocalDate date;

    private int consumed;

    private int burned;

    /** BREAKFAST, LUNCH, DINNER, SNACK, WORKOUT, OTHER */
    private String mealType;

    private String note;
}
