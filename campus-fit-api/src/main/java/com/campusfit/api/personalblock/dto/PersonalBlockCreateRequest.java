package com.campusfit.api.personalblock.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record PersonalBlockCreateRequest(
        @NotNull Integer year,
        @NotNull TermSeason termSeason,
        @NotBlank @Size(max = 100) String title,
        @NotNull DayOfWeekEnum dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String color) {
}
