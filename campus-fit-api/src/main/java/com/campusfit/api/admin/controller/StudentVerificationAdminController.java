package com.campusfit.api.admin.controller;

import com.campusfit.api.admin.dto.VerificationReviewRequest;
import com.campusfit.api.admin.service.StudentVerificationService;
import com.campusfit.api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/student-verifications")
@RequiredArgsConstructor
public class StudentVerificationAdminController {

    private final StudentVerificationService verificationService;

    @PatchMapping("/{verificationId}")
    public ResponseEntity<ApiResponse<Void>> review(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = Long.valueOf(userDetails.getUsername());
        verificationService.review(verificationId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
