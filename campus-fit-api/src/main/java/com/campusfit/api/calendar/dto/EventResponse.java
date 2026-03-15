package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.EventCategory;
import com.campusfit.api.domain.Event;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        EventCategory category,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean allDay,
        String description,
        LocalDateTime remindAt,
        String color,
        LocalDateTime createdAt) {

    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(),
                e.getTitle(),
                e.getCategory(),
                e.getStartAt(),
                e.getEndAt(),
                e.getAllDay(),
                e.getDescription(),
                e.getRemindAt(),
                e.getColor(),
                e.getCreatedAt());
    }
}
