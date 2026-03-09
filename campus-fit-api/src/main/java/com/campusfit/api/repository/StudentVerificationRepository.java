package com.campusfit.api.repository;

import com.campusfit.api.common.enums.VerificationStatus;
import com.campusfit.api.domain.StudentVerification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {
    List<StudentVerification> findByStatus(VerificationStatus status);

    List<StudentVerification> findByUserId(Long userId);
}
