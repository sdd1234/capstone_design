package com.campusfit.api.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationReviewRequest(
        @NotBlank String status, // APPROVED or REJECTED
        String rejectReason) {
}
