package com.campusfit.api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false, unique = true)
    private TimetablePreference preference;

    private Integer minCredits;
    private Integer maxCredits;
    private Integer targetCredits;

    /** 전공 목표 학점 (null이면 제한 없음) */
    private Integer targetMajorCredits;

    /** 교양 목표 학점 (null이면 제한 없음) */
    private Integer targetGeneralCredits;

    /** 원격(온라인) 목표 학점 (null이면 제한 없음) */
    private Integer targetRemoteCredits;
}
