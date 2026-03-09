package com.campusfit.api.repository;

import com.campusfit.api.domain.University;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByName(String name);
}
