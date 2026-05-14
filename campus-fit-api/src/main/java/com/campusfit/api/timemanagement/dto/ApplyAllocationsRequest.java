package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 추천 결과를 일괄 적용. weekStart 미제공 시 서버가 ISO 현재 주 월요일로 default.
 * 같은 weekStart + goalId의 기존 적용분만 교체.
 */
public record ApplyAllocationsRequest(
        LocalDate weekStart,
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull Long goalId,
            @NotNull DayOfWeekEnum dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) {}
}
