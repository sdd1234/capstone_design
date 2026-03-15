package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.TaskStatus;
import com.campusfit.api.domain.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        LocalDate scheduledDate,
        LocalDateTime dueAt,
        String category,
        LocalDateTime remindAt,
        Long linkedEventId,
        LocalDateTime createdAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getStatus(),
                t.getScheduledDate(),
                t.getDueAt(),
                t.getCategory(),
                t.getRemindAt(),
                t.getLinkedEvent() != null ? t.getLinkedEvent().getId() : null,
                t.getCreatedAt());
    }
}
