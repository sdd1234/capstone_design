package com.campusfit.api.domain;

import com.campusfit.api.common.enums.TermSeason;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timetable_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetablePreference {

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

    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PreferredTimeRange> timeRanges = new ArrayList<>();

    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DesiredCourse> desiredCourses = new ArrayList<>();

    @OneToOne(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private CreditPolicy creditPolicy;

    @OneToOne(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private PreferenceOption preferenceOption;

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
