package com.campusfit.api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "desired_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesiredCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false)
    private TimetablePreference preference;

    private Long courseId;

    @Column(length = 200)
    private String rawText;

    @Builder.Default
    private Integer priority = 0;
}
