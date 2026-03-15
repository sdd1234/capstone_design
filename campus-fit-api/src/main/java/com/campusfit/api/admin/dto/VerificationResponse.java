package com.campusfit.api.admin.dto;

import com.campusfit.api.domain.StudentVerification;
import java.time.LocalDateTime;

public record VerificationResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String verificationType,
        String status,
        String rejectReason,
        String note,
        Long fileId,
        String originalFileName,
        String mimeType,
        Long fileSize,
        String reviewedByName,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt) {

    public static VerificationResponse from(StudentVerification sv) {
        return new VerificationResponse(
                sv.getId(),
                sv.getUser().getId(),
                sv.getUser().getName(),
                sv.getUser().getEmail(),
                sv.getVerificationType(),
                sv.getStatus().name(),
                sv.getRejectReason(),
                sv.getNote(),
                sv.getFile().getId(),
                sv.getFile().getOriginalName(),
                sv.getFile().getMimeType(),
                sv.getFile().getSize(),
                sv.getReviewedBy() != null ? sv.getReviewedBy().getName() : null,
                sv.getCreatedAt(),
                sv.getUpdatedAt());
    }
}
