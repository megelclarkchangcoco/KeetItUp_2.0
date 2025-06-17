package com.example.keetitup_20;

import java.time.LocalDate;

public class CalendarDay {
    private final LocalDate date;       // full LocalDate
    private final String dayInitial;    // “S”, “M”, “T”, etc.
    private final int dayOfMonth;       // 1–31

    public CalendarDay(LocalDate date) {
        this.date = date;
        this.dayOfMonth = date.getDayOfMonth();
        // DayOfWeek.getDisplayName could also be used, but we only need single char:
        this.dayInitial = date.getDayOfWeek().toString().substring(0, 1);
        // (e.g. “MONDAY” → “M”, “TUESDAY” → “T”)
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDayInitial() {
        return dayInitial;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }
}