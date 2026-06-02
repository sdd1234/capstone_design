package com.campusfit.api.personalblock.dto;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.PersonalBlock;
import java.time.LocalTime;

public record PersonalBlockResponse(
        Long id,
        Integer year,
        TermSeason termSeason,
        String title,
        DayOfWeekEnum dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String color) {
    public static PersonalBlockResponse from(PersonalBlock b) {
        return new PersonalBlockResponse(
                b.getId(),
                b.getYear(),
                b.getTermSeason(),
                b.getTitle(),
                b.getDayOfWeek(),
                b.getStartTime(),
                b.getEndTime(),
                b.getColor());
    }
}
