package com.campusfit.api.ai.scoring;

import com.campusfit.api.domain.Lecture;
import java.util.List;

/**
 * 점수와 추천 사유가 부착된 시간표 후보.
 */
public record ScoredPlan(
        List<Lecture> lectures,
        Persona persona,
        double score,
        List<String> reasons) {
}
