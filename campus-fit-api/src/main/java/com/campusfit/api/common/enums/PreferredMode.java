package com.campusfit.api.common.enums;

/**
 * ActivityGoal 선호 시간대의 의미.
 *  - WINDOW: target_hours_per_week 시간을 preferred 윈도우 안에 분산 배치 (현재 default 동작)
 *  - FIXED:  매주 preferred 요일·시작·끝 그대로 1세션 고정 — target_hours 무시
 */
public enum PreferredMode {
    WINDOW,
    FIXED
}
