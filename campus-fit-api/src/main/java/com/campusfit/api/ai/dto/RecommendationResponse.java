package com.campusfit.api.ai.dto;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.AiTimetableRecommendation;
import com.campusfit.api.domain.RecommendationCandidate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record RecommendationResponse(
        Long id,
        Integer year,
        TermSeason termSeason,
        LocalDateTime createdAt,
        List<CandidateDto> candidates) {

    public record CandidateDto(
            Long id,
            Integer rank,
            Integer totalCredits,
            String persona,
            String personaLabel,
            Double score,
            List<String> reasons,
            List<LectureResponse> lectures) {

        public static CandidateDto from(RecommendationCandidate c) {
            List<String> reasons = (c.getReasonsText() == null || c.getReasonsText().isBlank())
                    ? Collections.emptyList()
                    : Arrays.asList(c.getReasonsText().split("\n"));
            return new CandidateDto(
                    c.getId(),
                    c.getRank(),
                    c.getTotalCredits(),
                    c.getPersona(),
                    c.getPersonaLabel(),
                    c.getScore(),
                    reasons,
                    c.getLectures().stream().map(LectureResponse::from).toList());
        }
    }

    public static RecommendationResponse from(AiTimetableRecommendation r) {
        return new RecommendationResponse(
                r.getId(),
                r.getYear(),
                r.getTermSeason(),
                r.getCreatedAt(),
                r.getCandidates().stream().map(CandidateDto::from).toList());
    }
}
