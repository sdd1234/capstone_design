package com.campusfit.api.repository;

import com.campusfit.api.domain.Task;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);

    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Task> findByUserIdAndScheduledDate(Long userId, LocalDate date);

    /** 캘린더 조회용: 사용자 + 예정일 범위 [from, to) */
    List<Task> findByUserIdAndScheduledDateGreaterThanEqualAndScheduledDateLessThan(
            Long userId, LocalDate from, LocalDate to);
}
