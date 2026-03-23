package com.campusfit.api.repository;

import com.campusfit.api.domain.AiTimetableRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTimetableRecommendationRepository extends JpaRepository<AiTimetableRecommendation, Long> {
    List<AiTimetableRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
