package com.campusfit.api.admin.service;

import com.campusfit.api.admin.dto.VerificationReviewRequest;
import com.campusfit.api.common.enums.UserStatus;
import com.campusfit.api.common.enums.VerificationStatus;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.StudentVerification;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.StudentVerificationRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentVerificationService {

    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public void review(Long verificationId, Long adminId, VerificationReviewRequest request) {
        StudentVerification sv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("재학생 인증 정보를 찾을 수 없습니다."));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> BusinessException.notFound("관리자를 찾을 수 없습니다."));

        VerificationStatus newStatus = VerificationStatus.valueOf(request.status());
        sv.setStatus(newStatus);
        sv.setRejectReason(request.rejectReason());
        sv.setReviewedBy(admin);

        // 유저 상태도 함께 변경
        User student = sv.getUser();
        if (newStatus == VerificationStatus.APPROVED) {
            student.setStatus(UserStatus.ACTIVE);
        } else if (newStatus == VerificationStatus.REJECTED) {
            student.setStatus(UserStatus.REJECTED);
        }
        userRepository.save(student);
    }
}
