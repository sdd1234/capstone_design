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

    /** 추천 페르소나 (LIGHT_WEEK / MAJOR_FOCUS / RELAXED) */
    @Column(length = 30)
    private String persona;

    /** 페르소나 라벨 (한글 노출용) */
    @Column(length = 50)
    private String personaLabel;

    /** TimetableScorer가 계산한 효용 점수 */
    private Double score;

    /** 추천 사유. 줄바꿈(\n)으로 구분된 텍스트로 저장 */
    @Lob
    private String reasonsText;

    @ManyToMany
    @JoinTable(name = "recommendation_candidate_lectures", joinColumns = @JoinColumn(name = "candidate_id"), inverseJoinColumns = @JoinColumn(name = "lecture_id"))
    @BatchSize(size = 50)
    @Builder.Default
    private List<Lecture> lectures = new ArrayList<>();
}
