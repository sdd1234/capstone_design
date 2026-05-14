package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** 부분 수정 — null 필드는 변경하지 않음. */
public record ActivityGoalUpdateRequest(
        @Size(max = 200) String title,
        ActivityCategory category,
        @Min(1) @Max(168) Integer targetHoursPerWeek,
        DayOfWeekEnum preferredDayOfWeek,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        PreferredMode preferredMode,
        @Size(max = 1000) String rawInput) {
}
