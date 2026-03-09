package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.EventCategory;
import com.campusfit.api.common.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarItemResponse(
        String itemType, // "EVENT" or "TASK"
        Long id,
        String title,
        // Event fields
        EventCategory eventCategory,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean allDay,
        String color,
        // Task fields
        TaskStatus taskStatus,
        LocalDate scheduledDate,
        LocalDateTime dueAt) {
}
