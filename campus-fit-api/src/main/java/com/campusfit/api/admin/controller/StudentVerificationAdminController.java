package com.campusfit.api.admin.controller;

import com.campusfit.api.admin.dto.VerificationResponse;
import com.campusfit.api.admin.dto.VerificationReviewRequest;
import com.campusfit.api.admin.service.StudentVerificationService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.domain.StudentVerification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "관리자 - 학생 인증 심사", description = "학생 인증 신청 목록 조회·파일 다운로드·승인·반려 API (ADMIN 전용)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/student-verifications")
@RequiredArgsConstructor
public class StudentVerificationAdminController {

    private final StudentVerificationService verificationService;

    @Operation(summary = "인증 신청 목록 조회 [ADMIN]", description = "학생 인증 신청 전체 목록을 반환합니다. status 파라미터로 상태 필터링이 가능합니다.\n\nstatus 값: PENDING · APPROVED · REJECTED")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VerificationResponse>>> list(
            @Parameter(description = "상태 필터 (PENDING / APPROVED / REJECTED), 미입력 시 전체") @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.list(status)));
    }

    @Operation(summary = "인증 신청 상세 조회 [ADMIN]", description = "특정 학생의 인증 신청 내용과 첨부 파일 메타데이터(파일명·크기·MIME타입)를 반환합니다.")
    @GetMapping("/{verificationId}")
    public ResponseEntity<ApiResponse<VerificationResponse>> detail(
            @Parameter(description = "인증 신청 ID") @PathVariable Long verificationId) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.detail(verificationId)));
    }

    @Operation(summary = "인증 파일 다운로드 [ADMIN]", description = "학생이 첨부한 학생증·교직원증 파일을 브라우저에서 inline으로 표시합니다.")
    @GetMapping("/{verificationId}/file")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "인증 신청 ID") @PathVariable Long verificationId) {
        StudentVerification sv = verificationService.getVerification(verificationId);
        Resource resource = verificationService.loadFile(verificationId);

        String mimeType = sv.getFile().getMimeType();
        MediaType mediaType = (mimeType != null)
                ? MediaType.parseMediaType(mimeType)
                : MediaType.APPLICATION_OCTET_STREAM;

        String originalName = sv.getFile().getOriginalName()
                .replaceAll("[\\r\\n]", ""); // CRLF injection 방지

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + originalName + "\"")
                .body(resource);
    }

    @Operation(summary = "인증 심사 처리 [ADMIN]", description = "인증 신청을 승인(APPROVED) 또는 반려(REJECTED) 처리합니다. reviewNote에 심사 코멘트를 남길 수 있습니다.")
    @PatchMapping("/{verificationId}")
    public ResponseEntity<ApiResponse<Void>> review(
            @Parameter(description = "인증 신청 ID") @PathVariable Long verificationId,
            @Valid @RequestBody VerificationReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = Long.valueOf(userDetails.getUsername());
        verificationService.review(verificationId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
