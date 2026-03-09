package com.campusfit.api.repository;

import com.campusfit.api.domain.AcademicCalendarEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Long> {
    List<AcademicCalendarEvent> findByUniversityIdAndYear(Long universityId, Integer year);

    List<AcademicCalendarEvent> findByYear(Integer year);
}
