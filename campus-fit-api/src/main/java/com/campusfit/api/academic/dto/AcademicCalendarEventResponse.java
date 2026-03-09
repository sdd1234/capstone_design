package com.campusfit.api.academic.dto;

import com.campusfit.api.domain.AcademicCalendarEvent;
import java.time.LocalDate;

public record AcademicCalendarEventResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String category,
        Integer year) {
    public static AcademicCalendarEventResponse from(AcademicCalendarEvent e) {
        return new AcademicCalendarEventResponse(
                e.getId(), e.getTitle(), e.getStartDate(), e.getEndDate(), e.getCategory(), e.getYear());
    }
}
