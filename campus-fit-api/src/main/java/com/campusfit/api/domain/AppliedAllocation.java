package com.campusfit.api.domain;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자가 "이 추천을 내 시간표에 적용"한 결과물.
 * 한 ActivityGoal이 여러 placement를 받을 수 있으므로 N행 가능.
 * 추천이 휘발하지 않고 영속되도록 분리된 엔티티.
 */
@Entity
@Table(name = "applied_allocations", indexes = {
        @Index(name = "idx_aa_user", columnList = "user_id"),
        @Index(name = "idx_aa_user_week", columnList = "user_id, week_start")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private ActivityGoal goal;

    /** 이 적용분이 속한 주 — 월요일 기준. 주별 영속·완료 추적의 키. */
    @Column(name = "week_start")
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 5)
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** 사용자가 "이거 진짜 했음" 체크. 주간 달성률 집계용. */
    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "applied_at", nullable = false)
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();
}
