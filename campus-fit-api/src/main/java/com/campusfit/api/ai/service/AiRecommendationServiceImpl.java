package com.campusfit.api.ai.service;

import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final AiTimetableRecommendationRepository recommendationRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendationResponse create(Long userId, RecommendationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            paramsJson = "{}";
        }

        AiTimetableRecommendation rec = AiTimetableRecommendation.builder()
                .user(user)
                .year(request.year())
                .termSeason(request.termSeason())
                .requestParamsJson(paramsJson)
                .build();

        // 간단한 추천 로직: 요청된 강좌 + 사용 가능한 강좌 중 일부를 조합
        List<Lecture> available = lectureRepository.findAll();
        if (!available.isEmpty()) {
            RecommendationCandidate candidate = RecommendationCandidate.builder()
                    .recommendation(rec)
                    .rank(1)
                    .totalCredits(available.stream()
                            .mapToInt(l -> l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0)
                            .sum())
                    .lectures(new ArrayList<>(available))
                    .build();
            rec.getCandidates().add(candidate);
        }

        return RecommendationResponse.from(recommendationRepository.save(rec));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> list(Long userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(RecommendationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse get(Long userId, Long recommendationId) {
        AiTimetableRecommendation rec = findOwned(userId, recommendationId);
        return RecommendationResponse.from(rec);
    }

    @Override
    public void delete(Long userId, Long recommendationId) {
        AiTimetableRecommendation rec = findOwned(userId, recommendationId);
        recommendationRepository.delete(rec);
    }

    private AiTimetableRecommendation findOwned(Long userId, Long recId) {
        AiTimetableRecommendation rec = recommendationRepository.findById(recId)
                .orElseThrow(() -> BusinessException.notFound("추천 결과를 찾을 수 없습니다."));
        if (!rec.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        return rec;
    }
}
