package com.campusfit.api.timetablepreference.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.enums.TimeRangeType;
import com.campusfit.api.domain.*;
import java.time.LocalTime;
import java.util.List;

public record TimetablePreferenceResponse(
        Long id,
        Integer year,
        TermSeason termSeason,
        List<TimeRangeDto> timeRanges,
        List<DesiredCourseDto> desiredCourses,
        CreditPolicyDto creditPolicy,
        PreferenceOptionDto options) {
    public record TimeRangeDto(Long id, TimeRangeType type, DayOfWeekEnum dayOfWeek, LocalTime startTime,
            LocalTime endTime) {
        public static TimeRangeDto from(PreferredTimeRange r) {
            return new TimeRangeDto(r.getId(), r.getType(), r.getDayOfWeek(), r.getStartTime(), r.getEndTime());
        }
    }

    public record DesiredCourseDto(Long id, Long courseId, String rawText, Integer priority) {
        public static DesiredCourseDto from(DesiredCourse d) {
            return new DesiredCourseDto(d.getId(), d.getCourseId(), d.getRawText(), d.getPriority());
        }
    }

    public record CreditPolicyDto(Integer minCredits, Integer maxCredits, Integer targetCredits,
            Integer targetMajorCredits, Integer targetGeneralCredits, Integer targetRemoteCredits) {
        public static CreditPolicyDto from(CreditPolicy cp) {
            if (cp == null)
                return null;
            return new CreditPolicyDto(cp.getMinCredits(), cp.getMaxCredits(), cp.getTargetCredits(),
                    cp.getTargetMajorCredits(), cp.getTargetGeneralCredits(), cp.getTargetRemoteCredits());
        }
    }

    public record PreferenceOptionDto(Boolean excludeMorning, Integer allowGapsMinutes, Integer maxDaysPerWeek,
            String dept, Boolean preferMajorOnly, Integer grade) {
        public static PreferenceOptionDto from(PreferenceOption po) {
            if (po == null)
                return null;
            return new PreferenceOptionDto(po.getExcludeMorning(), po.getAllowGapsMinutes(), po.getMaxDaysPerWeek(),
                    po.getDept(), po.getPreferMajorOnly(), po.getGrade());
        }
    }

    public static TimetablePreferenceResponse from(TimetablePreference p) {
        return new TimetablePreferenceResponse(
                p.getId(),
                p.getYear(),
                p.getTermSeason(),
                p.getTimeRanges().stream().map(TimeRangeDto::from).toList(),
                p.getDesiredCourses().stream().map(DesiredCourseDto::from).toList(),
                CreditPolicyDto.from(p.getCreditPolicy()),
                PreferenceOptionDto.from(p.getPreferenceOption()));
    }
}
