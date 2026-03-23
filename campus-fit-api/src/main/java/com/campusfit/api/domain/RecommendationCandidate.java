package com.campusfit.api.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendation_candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private AiTimetableRecommendation recommendation;

    @Column(name = "candidate_rank", nullable = false)
    private Integer rank;

    private Integer totalCredits;

    @ManyToMany
    @JoinTable(name = "recommendation_candidate_lectures", joinColumns = @JoinColumn(name = "candidate_id"), inverseJoinColumns = @JoinColumn(name = "lecture_id"))
    @BatchSize(size = 50)
    @Builder.Default
    private List<Lecture> lectures = new ArrayList<>();
}
