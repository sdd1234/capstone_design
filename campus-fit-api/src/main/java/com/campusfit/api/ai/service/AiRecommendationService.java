package com.campusfit.api.ai.service;

import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import java.util.List;

public interface AiRecommendationService {
    RecommendationResponse create(Long userId, RecommendationRequest request);

    List<RecommendationResponse> list(Long userId);

    RecommendationResponse get(Long userId, Long recommendationId);

    void delete(Long userId, Long recommendationId);
}
