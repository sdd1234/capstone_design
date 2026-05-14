package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 추천 결과 항목.
 *  - sourceType=GOAL: ActivityGoal 추천. goalId/goalTitle/category 채워짐.
 *  - sourceType=TASK: 일회성 Task 추천 (마감 임박). taskId 채워지고 goalId=null. category는 OTHER로 표시.
 *  - 빈 슬롯 부족 placeholder는 dayOfWeek=null + reason 메시지만.
 */
public record ActivityAllocationResponse(
        SourceType sourceType,
        Long goalId,
        Long taskId,
        String goalTitle,
        ActivityCategory category,
        DayOfWeekEnum dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int durationMinutes,
        LocalDate scheduledDate,
        String reason) {

    public enum SourceType { GOAL, TASK }

    /** Goal 추천 헬퍼 */
    public static ActivityAllocationResponse forGoal(
            Long goalId, String title, ActivityCategory category,
            DayOfWeekEnum day, LocalTime start, LocalTime end, int mins, String reason) {
        return new ActivityAllocationResponse(SourceType.GOAL, goalId, null, title, category,
                day, start, end, mins, null, reason);
    }

    /** Task 추천 헬퍼 */
    public static ActivityAllocationResponse forTask(
            Long taskId, String title, DayOfWeekEnum day, LocalDate date,
            LocalTime start, LocalTime end, int mins, String reason) {
        return new ActivityAllocationResponse(SourceType.TASK, null, taskId, title, ActivityCategory.OTHER,
                day, start, end, mins, date, reason);
    }
}
