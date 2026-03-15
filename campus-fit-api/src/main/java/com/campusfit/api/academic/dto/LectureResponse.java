package com.campusfit.api.academic.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.Lecture;
import com.campusfit.api.domain.LectureSchedule;
import java.time.LocalTime;
import java.util.List;

public record LectureResponse(
        Long id,
        Long courseId,
        String courseName,
        Integer credits,
        Integer year,
        TermSeason termSeason,
        String professor,
        String room,
        Boolean isRemote,
        String lectureNumber,
        String area,
        String campus,
        String category,
        List<ScheduleDto> schedules) {
    public record ScheduleDto(DayOfWeekEnum dayOfWeek, LocalTime startTime, LocalTime endTime) {
        public static ScheduleDto from(LectureSchedule s) {
            return new ScheduleDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime());
        }
    }

    public static LectureResponse from(Lecture l) {
        return new LectureResponse(
                l.getId(),
                l.getCourse().getId(),
                l.getCourse().getName(),
                l.getCourse().getCredits(),
                l.getYear(),
                l.getTermSeason(),
                l.getProfessor(),
                l.getRoom(),
                l.getIsRemote(),
                l.getLectureNumber(),
                l.getArea(),
                l.getCampus(),
                l.getCourse().getCategory(),
                l.getSchedules().stream().map(ScheduleDto::from).toList());
    }
}
