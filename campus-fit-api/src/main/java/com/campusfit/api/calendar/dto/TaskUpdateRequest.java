package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskUpdateRequest(
        String title,
        TaskStatus status,
        LocalDate scheduledDate,
        LocalDateTime dueAt,
        String category,
        LocalDateTime remindAt,
        Long linkedEventId) {
}
