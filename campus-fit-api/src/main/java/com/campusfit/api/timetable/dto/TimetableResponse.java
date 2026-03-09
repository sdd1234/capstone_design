package com.campusfit.api.timetable.dto;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.enums.TimetableStatus;
import com.campusfit.api.domain.Timetable;
import java.time.LocalDateTime;
import java.util.List;

public record TimetableResponse(
        Long id,
        Integer year,
        TermSeason termSeason,
        String title,
        TimetableStatus status,
        Long sourceRecommendationId,
        List<LectureResponse> lectures,
        LocalDateTime createdAt) {
    public static TimetableResponse from(Timetable t) {
        return new TimetableResponse(
                t.getId(),
                t.getYear(),
                t.getTermSeason(),
                t.getTitle(),
                t.getStatus(),
                t.getSourceRecommendationId(),
                t.getItems().stream().map(item -> LectureResponse.from(item.getLecture())).toList(),
                t.getCreatedAt());
    }
}
