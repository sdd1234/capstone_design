package com.campusfit.api.domain;

import com.campusfit.api.common.enums.TermSeason;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_import_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TermSeason termSeason;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Integer importedCount;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt = LocalDateTime.now();
}
