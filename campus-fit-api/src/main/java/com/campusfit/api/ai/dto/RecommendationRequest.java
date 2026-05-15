package com.campusfit.api.ai.dto;

import com.campusfit.api.common.enums.TermSeason;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecommendationRequest(
        @NotNull Integer year,
        @NotNull TermSeason termSeason,
        List<Long> preferredLectureIds,
        List<Long> excludeLectureIds,
        /** 이미 시간표에 등록된 강의(채플·필수교양 등). 추천에서 시간 충돌 회피 + 추천 결과에 포함하지 않음 */
        List<Long> occupiedLectureIds) {
}
