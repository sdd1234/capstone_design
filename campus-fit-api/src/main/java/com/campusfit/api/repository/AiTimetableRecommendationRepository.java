package com.campusfit.api.repository;

import com.campusfit.api.domain.AiTimetableRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTimetableRecommendationRepository extends JpaRepository<AiTimetableRecommendation, Long> {
    @EntityGraph(attributePaths = { "candidates", "candidates.lectures", "candidates.lectures.schedules" })
    List<AiTimetableRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
