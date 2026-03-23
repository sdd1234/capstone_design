package com.campusfit.api.repository;

import com.campusfit.api.domain.Timetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByUserIdOrderByCreatedAtDesc(Long userId);
}
