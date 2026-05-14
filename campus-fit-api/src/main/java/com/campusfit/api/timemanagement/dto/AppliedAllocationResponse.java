package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.domain.AppliedAllocation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppliedAllocationResponse(
        Long id,
        Long goalId,
        String goalTitle,
        ActivityCategory category,
        LocalDate weekStart,
        DayOfWeekEnum dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        Boolean isCompleted,
        LocalDateTime completedAt) {

    public static AppliedAllocationResponse from(AppliedAllocation a) {
        int mins = (int) java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        return new AppliedAllocationResponse(
                a.getId(),
                a.getGoal().getId(),
                a.getGoal().getTitle(),
                a.getGoal().getCategory(),
                a.getWeekStart(),
                a.getDayOfWeek(),
                a.getStartTime(),
                a.getEndTime(),
                mins,
                Boolean.TRUE.equals(a.getIsCompleted()),
                a.getCompletedAt());
    }
}
