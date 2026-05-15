package com.campusfit.api.ai.scoring;

/**
 * 시간표 추천 페르소나. 후보 3개를 각기 다른 컨셉으로 생성하기 위한 가중치 프리셋.
 */
public enum Persona {
    /** 공강일 최대화 - 주 4일 이하 등교를 목표 */
    LIGHT_WEEK("공강 많은 시간표", "주 등교일을 최소화한 가벼운 한 주"),
    /** 전공 집중 - 전공 학점 비중 최대화 */
    MAJOR_FOCUS("전공 집중", "전공 학점 비중을 끌어올린 빡센 학기"),
    /** 여유 - 오전 첫 교시 회피 + 연강 패널티 강화 */
    RELAXED("여유로운 시간표", "오전 첫 교시 없이 연강도 적은 여유 위주");

    private final String label;
    private final String description;

    Persona(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public ScoreWeights weights() {
        return switch (this) {
            case LIGHT_WEEK -> new ScoreWeights(3.0, 1.0, 1.0, 0.8, 0.5, 0.5);
            case MAJOR_FOCUS -> new ScoreWeights(0.5, 1.0, 0.5, 0.5, 0.3, 3.0);
            case RELAXED -> new ScoreWeights(1.0, 1.2, 2.0, 1.5, 2.0, 0.5);
        };
    }
}
