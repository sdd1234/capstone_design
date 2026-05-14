package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import java.time.LocalTime;

/**
 * ActivityGoal.rawInput (자연어 메모) → 구조화된 필드 추출 인터페이스.
 *
 * 현재는 NoOp 구현. ChatGPT 연동 시 OpenAiRawInputParser 같은 구현체 주입.
 * 통합 지점: ActivityGoalServiceImpl.create / update 안에서 호출 → 사용자가
 * "운동은 친구랑 같이, 저녁 늦은 시간은 피하고 싶어" 같은 메모를 적으면
 * 카테고리/요일/시간을 자동 추론.
 */
public interface RawInputParser {

    /** 파싱 결과 — null 필드는 추론 불가 */
    record Parsed(
            ActivityCategory category,
            DayOfWeekEnum preferredDayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            String reasonNote) {}

    Parsed parse(String rawInput);
}
