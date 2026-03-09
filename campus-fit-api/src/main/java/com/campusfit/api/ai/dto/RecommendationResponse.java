package com.campusfit.api.ai.dto;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.AiTimetableRecommendation;
import com.campusfit.api.domain.RecommendationCandidate;
import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long id,
        Integer year,
        TermSeason termSeason,
        LocalDateTime createdAt,
        List<CandidateDto> candidates) {
    public record CandidateDto(Long id, Integer rank, Integer totalCredits, List<LectureResponse> lectures) {
        public static CandidateDto from(RecommendationCandidate c) {
            return new CandidateDto(
                    c.getId(),
                    c.getRank(),
                    c.getTotalCredits(),
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
