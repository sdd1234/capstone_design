package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import com.campusfit.api.domain.ActivityGoal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ActivityGoalResponse(
        Long id,
        String title,
        ActivityCategory category,
        Integer targetHoursPerWeek,
        DayOfWeekEnum preferredDayOfWeek,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        PreferredMode preferredMode,
        String rawInput,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ActivityGoalResponse from(ActivityGoal g) {
        return new ActivityGoalResponse(
                g.getId(),
                g.getTitle(),
                g.getCategory(),
                g.getTargetHoursPerWeek(),
                g.getPreferredDayOfWeek(),
                g.getPreferredStartTime(),
                g.getPreferredEndTime(),
                g.getPreferredMode(),
                g.getRawInput(),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }
}
