package com.campusfit.api.timemanagement.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import java.time.LocalTime;

public record BlankSlotResponse(
        DayOfWeekEnum dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int durationMinutes) {
}
