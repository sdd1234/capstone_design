package com.campusfit.api.domain;

import com.campusfit.api.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    private LocalDate scheduledDate;

    private LocalDateTime dueAt;

    @Column(length = 50)
    private String category;

    private LocalDateTime remindAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_event_id")
    private Event linkedEvent;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
