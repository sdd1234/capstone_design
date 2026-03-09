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
}
