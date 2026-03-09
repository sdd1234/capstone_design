package com.campusfit.api.admin.controller;

import com.campusfit.api.common.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/student-verifications")
@Validated
public class StudentVerificationAdminController {

    @PatchMapping("/{verificationId}")
    public ResponseEntity<ApiResponse<String>> updateVerification(
        @PathVariable Long verificationId,
        @RequestBody UpdateStudentVerificationRequest request
    ) {
        String msg = "verificationId=" + verificationId + ", status=" + request.status();
        return ResponseEntity.ok(ApiResponse.ok(msg));
    }

    public record UpdateStudentVerificationRequest(
        @NotBlank String status,
        String rejectReason
    ) {
    }
}
