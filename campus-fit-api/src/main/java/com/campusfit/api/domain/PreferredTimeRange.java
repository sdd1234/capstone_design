package com.campusfit.api.domain;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TimeRangeType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "preferred_time_ranges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferredTimeRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false)
    private TimetablePreference preference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TimeRangeType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private DayOfWeekEnum dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}
