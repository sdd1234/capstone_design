package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record ActivityGoalCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull ActivityCategory category,
        @NotNull @Min(1) @Max(168) Integer targetHoursPerWeek,
        DayOfWeekEnum preferredDayOfWeek,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        PreferredMode preferredMode,
        @Size(max = 1000) String rawInput) {
}
