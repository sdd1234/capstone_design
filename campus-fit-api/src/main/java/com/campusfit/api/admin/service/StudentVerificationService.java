package com.campusfit.api.admin.service;

import com.campusfit.api.admin.dto.VerificationReviewRequest;
import com.campusfit.api.admin.dto.VerificationResponse;
import com.campusfit.api.common.enums.UserStatus;
import com.campusfit.api.common.enums.VerificationStatus;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.StudentVerification;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.FileRepository;
import com.campusfit.api.repository.StudentVerificationRepository;
import com.campusfit.api.repository.UserRepository;
import com.campusfit.api.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentVerificationService {

    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<VerificationResponse> list(String status) {
        List<StudentVerification> list = (status != null && !status.isBlank())
                ? verificationRepository.findByStatus(VerificationStatus.valueOf(status))
                : verificationRepository.findAll();
        return list.stream().map(VerificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VerificationResponse detail(Long verificationId) {
        StudentVerification sv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("인증 정보를 찾을 수 없습니다."));
        return VerificationResponse.from(sv);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long verificationId) {
        StudentVerification sv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("인증 정보를 찾을 수 없습니다."));
        return fileStorageService.loadAsResource(sv.getFile().getStoredPath());
    }

    @Transactional(readOnly = true)
    public StudentVerification getVerification(Long verificationId) {
        return verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("인증 정보를 찾을 수 없습니다."));
    }

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
