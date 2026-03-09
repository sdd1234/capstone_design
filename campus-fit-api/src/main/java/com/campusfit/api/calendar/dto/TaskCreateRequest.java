package com.campusfit.api.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskCreateRequest(
        @NotBlank String title,
        LocalDate scheduledDate,
        LocalDateTime dueAt,
        String category,
        LocalDateTime remindAt,
        Long linkedEventId) {
}
