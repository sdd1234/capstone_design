package com.campusfit.api.domain;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 시간표 그리드에 표시되는 알바·개인 반복 일정.
 * Timetable(강의)과 독립적으로 유저+학기 단위로 존재한다 → 대표 시간표가 없어도(방학 등) 추가 가능.
 * 매주 반복되는 규칙(요일+시간)으로 저장하고, 화면에서 매주 펼쳐 보여준다.
 */
@Entity
@Table(name = "personal_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "academic_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TermSeason termSeason;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private DayOfWeekEnum dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** 그리드 표시 색상 (예: #f97c4f). 미지정 시 프론트가 기본색 사용. */
    @Column(length = 20)
    private String color;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
