package com.campusfit.api.timetablepreference.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.enums.TimeRangeType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record TimetablePreferenceRequest(
                @NotNull Integer year,
                @NotNull TermSeason termSeason,
                List<TimeRangeDto> timeRanges,
                List<DesiredCourseDto> desiredCourses,
                CreditPolicyDto creditPolicy,
                PreferenceOptionDto options) {
        public record TimeRangeDto(
                        @NotNull TimeRangeType type,
                        DayOfWeekEnum dayOfWeek,
                        @NotNull LocalTime startTime,
                        @NotNull LocalTime endTime) {
        }

        public record DesiredCourseDto(
                        Long courseId,
                        String rawText,
                        Integer priority) {
        }

        public record CreditPolicyDto(
                        Integer minCredits,
                        Integer maxCredits,
                        Integer targetCredits,
                        Integer targetMajorCredits,
                        Integer targetGeneralCredits,
                        Integer targetRemoteCredits) {
        }

        public record PreferenceOptionDto(
                        Boolean excludeMorning,
                        Integer allowGapsMinutes,
                        Integer maxDaysPerWeek,
                        String dept,
                        Boolean preferMajorOnly,
                        Integer grade) {
        }
}
