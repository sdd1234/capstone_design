package com.campusfit.api.ai.dto;

import com.campusfit.api.common.enums.TermSeason;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecommendationRequest(
        @NotNull Integer year,
        @NotNull TermSeason termSeason,
        List<Long> preferredLectureIds,
        List<Long> excludeLectureIds) {
}
