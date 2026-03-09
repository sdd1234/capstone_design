package com.campusfit.api.timetable.dto;

import com.campusfit.api.common.enums.TermSeason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TimetableCreateRequest(
        @NotNull Integer year,
        @NotNull TermSeason termSeason,
        @NotBlank String title,
        List<Long> lectureIds,
        Long sourceRecommendationId) {
}
