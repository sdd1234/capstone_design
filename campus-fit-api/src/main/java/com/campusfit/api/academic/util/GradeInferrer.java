package com.campusfit.api.academic.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 강의 데이터에 target_grade가 비어있을 때, 강의명 핵심어 + 시리즈 번호로
 * 학년을 추정한다. 한국 공학·자연계 학부의 일반적인 커리큘럼 패턴 기반 휴리스틱.
 *
 * 정확도 한계: 학과/학교마다 다르므로 ~70% 수준. targetGrade가 명시되어 있으면
 * 그 값을 우선 사용해야 한다. 이 추정은 fallback 전용.
 */
public final class GradeInferrer {

    private GradeInferrer() {}

    /** 키워드 포함 → 학년. 더 구체적인 키워드(긴 것)를 앞에 두기 위해 LinkedHashMap */
    private static final Map<String, Integer> KEYWORD_TO_GRADE = new LinkedHashMap<>();
    static {
        // 4학년
        KEYWORD_TO_GRADE.put("졸업프로젝트", 4);
        KEYWORD_TO_GRADE.put("졸업설계", 4);
        KEYWORD_TO_GRADE.put("졸업작품", 4);
        KEYWORD_TO_GRADE.put("종합설계", 4);
        KEYWORD_TO_GRADE.put("컴파일러", 4);
        KEYWORD_TO_GRADE.put("분산시스템", 4);
        KEYWORD_TO_GRADE.put("딥러닝", 4);
        KEYWORD_TO_GRADE.put("강화학습", 4);
        KEYWORD_TO_GRADE.put("정보보호", 4);
        KEYWORD_TO_GRADE.put("암호학", 4);
        // 3학년
        KEYWORD_TO_GRADE.put("운영체제", 3);
        KEYWORD_TO_GRADE.put("알고리즘", 3);
        KEYWORD_TO_GRADE.put("데이터베이스", 3);
        KEYWORD_TO_GRADE.put("네트워크", 3);
        KEYWORD_TO_GRADE.put("컴퓨터네트워크", 3);
        KEYWORD_TO_GRADE.put("소프트웨어공학", 3);
        KEYWORD_TO_GRADE.put("웹프로그래밍", 3);
        KEYWORD_TO_GRADE.put("모바일", 3);
        KEYWORD_TO_GRADE.put("인공지능", 3);
        KEYWORD_TO_GRADE.put("머신러닝", 3);
        KEYWORD_TO_GRADE.put("기계학습", 3);
        KEYWORD_TO_GRADE.put("영상처리", 3);
        KEYWORD_TO_GRADE.put("컴퓨터그래픽스", 3);
        // 2학년
        KEYWORD_TO_GRADE.put("자료구조", 2);
        KEYWORD_TO_GRADE.put("이산수학", 2);
        KEYWORD_TO_GRADE.put("객체지향", 2);
        KEYWORD_TO_GRADE.put("자바프로그래밍", 2);
        KEYWORD_TO_GRADE.put("C++프로그래밍", 2);
        KEYWORD_TO_GRADE.put("컴퓨터구조", 2);
        KEYWORD_TO_GRADE.put("디지털논리", 2);
        KEYWORD_TO_GRADE.put("논리회로", 2);
        KEYWORD_TO_GRADE.put("선형대수", 2);
        KEYWORD_TO_GRADE.put("회로이론", 2);
        KEYWORD_TO_GRADE.put("공업수학", 2);
        // 1학년
        KEYWORD_TO_GRADE.put("프로그래밍기초", 1);
        KEYWORD_TO_GRADE.put("프로그래밍언어", 1);
        KEYWORD_TO_GRADE.put("프로그래밍입문", 1);
        KEYWORD_TO_GRADE.put("미적분학", 1);
        KEYWORD_TO_GRADE.put("일반물리", 1);
        KEYWORD_TO_GRADE.put("일반화학", 1);
        KEYWORD_TO_GRADE.put("일반생물", 1);
        KEYWORD_TO_GRADE.put("대학수학", 1);
    }

    /**
     * 강의명에서 학년 추정. 정보 부족이면 null 반환.
     *
     * 우선순위:
     *  1) 캡스톤·종합설계 류는 시리즈 번호로 분기 (1)→3, (2)→4
     *  2) 키워드 매핑 테이블 (긴 키워드 먼저)
     */
    public static Integer inferFromCourseName(String name) {
        if (name == null || name.isBlank()) return null;

        boolean isCapstone = name.contains("캡스톤") || name.contains("종합설계")
                || name.contains("졸업프로젝트") || name.contains("졸업설계") || name.contains("졸업작품");
        if (isCapstone) {
            // 시리즈 분기: (2) / II / 2 → 4학년, (1) / I / 1 → 3학년
            if (name.contains("(2)") || name.endsWith("2") || name.contains("II")) return 4;
            if (name.contains("(1)") || name.endsWith("1") || name.contains(" I ") || name.endsWith("I")) return 3;
            return 4; // 시리즈 표기 없으면 일반적으로 4학년 졸업 작품
        }

        for (Map.Entry<String, Integer> e : KEYWORD_TO_GRADE.entrySet()) {
            if (name.contains(e.getKey())) return e.getValue();
        }
        return null;
    }
}
