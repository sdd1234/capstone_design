package com.campusfit.api.domain;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자의 주간 반복 활동 목표 (운동/공부/식사 등).
 *
 * Task / Event 와 차이:
 *  - Task: single-instance, scheduledDate 단일 날짜
 *  - Event: single-instance, startAt/endAt 절대 일시
 *  - ActivityGoal: weekly recurring goal — "주에 N시간", 선호 요일/시간대(nullable)
 *
 * preferred_* 컬럼 의미: 선호 윈도우(window) — target_hours_per_week 시간을
 * 이 윈도우 안에 분산 배치한다. "매주 월 18-20시 고정"이 아니라 "월 18-20시 안에 우선".
 */
@Entity
@Table(name = "activity_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ActivityCategory category = ActivityCategory.OTHER;

    /** 주당 목표 시간 (예: 운동 3시간/주) */
    @Column(nullable = false)
    private Integer targetHoursPerWeek;

    /** 선호 요일 — null이면 요일 무관 */
    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private DayOfWeekEnum preferredDayOfWeek;

    /** 선호 시작 시각 — null이면 시간 무관 */
    private LocalTime preferredStartTime;

    /** 선호 끝 시각 — null이면 시간 무관 */
    private LocalTime preferredEndTime;

    /**
     * 선호 시간대 의미 — WINDOW(분산) / FIXED(매주 그 시간 고정).
     * DB nullable로 두는 이유: globally_quoted_identifiers=true 환경에서
     * NOT NULL columnDefinition을 ALTER TABLE 합치면 syntax error.
     * 코드에서 항상 default(WINDOW) 채워서 실질 NOT NULL 보장.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private PreferredMode preferredMode = PreferredMode.WINDOW;

    /** 자연어 메모 — 사용자가 자유롭게 입력하는 부가 설명. 추후 ChatGPT 파서 입력 */
    @Column(length = 1000)
    private String rawInput;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
