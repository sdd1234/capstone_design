package com.campusfit.api.calendar.dto;

import com.campusfit.api.common.enums.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank String title,
        @NotNull EventCategory category,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        Boolean allDay,
        String description,
        LocalDateTime remindAt,
        String color) {
}
