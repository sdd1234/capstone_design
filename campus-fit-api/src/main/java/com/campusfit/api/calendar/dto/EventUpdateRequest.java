package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.EventCategory;
import java.time.LocalDateTime;

public record EventUpdateRequest(
        String title,
        EventCategory category,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean allDay,
        String description,
        LocalDateTime remindAt,
        String color) {
}
