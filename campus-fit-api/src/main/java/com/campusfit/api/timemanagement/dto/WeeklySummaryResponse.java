package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import java.time.LocalDate;
import java.util.List;

/**
 * 한 주의 활동별 달성률 집계.
 *
 * 예: 운동 → 적용 180분, 완료 120분 (66%)
 *     공부 → 적용 360분, 완료 360분 (100%)
 */
public record WeeklySummaryResponse(
        LocalDate weekStart,
        Integer totalAppliedMinutes,
        Integer totalCompletedMinutes,
        List<GoalProgress> byGoal) {

    public record GoalProgress(
            Long goalId,
            String goalTitle,
            ActivityCategory category,
            Integer targetHoursPerWeek,
            Integer appliedMinutes,
            Integer completedMinutes,
            Integer completionPercent) {}
}
