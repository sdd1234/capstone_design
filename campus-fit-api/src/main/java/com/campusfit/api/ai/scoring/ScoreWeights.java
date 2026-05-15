package com.campusfit.api.ai.scoring;

/**
 * 시간표 점수 가중치. 페르소나마다 다른 프리셋을 적용해 후보의 컨셉을 차별화한다.
 */
public record ScoreWeights(
        double wEmptyDays,
        double wPreferTime,
        double wConsecutivePenalty,
        double wLunchBreak,
        double wFirstPeriodPenalty,
        double wMajorRatio) {
}
