package com.campusfit.api.repository;

import com.campusfit.api.domain.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUniversityId(Long universityId);

    List<Course> findByNameContainingIgnoreCase(String keyword);
}
