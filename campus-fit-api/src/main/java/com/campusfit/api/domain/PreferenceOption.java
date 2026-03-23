package com.campusfit.api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "preference_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false, unique = true)
    private TimetablePreference preference;

    @Builder.Default
    private Boolean excludeMorning = false;

    private Integer allowGapsMinutes;

    private Integer maxDaysPerWeek;

    /** 선호 학과 (예: "컴퓨터공학과") - AI 추천 시 해당 학과 전공 우선 */
    @Column(length = 100)
    private String dept;

    /** 전공 위주로만 채우기 옵션 */
    @Builder.Default
    private Boolean preferMajorOnly = false;

    /** 사용자 학년 (1~4, null이면 학년 필터 안 함) */
    private Integer grade;
}
