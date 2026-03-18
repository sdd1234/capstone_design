package com.campusfit.api.auth.controller;

import com.campusfit.api.auth.dto.*;
import com.campusfit.api.auth.service.AuthService;
import com.campusfit.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "인증", description = "회원가입·로그인·토큰 갱신 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일·비밀번호로 계정을 생성합니다. 학생증 파일(verificationFile)을 첨부하면 인증 신청이 함께 등록됩니다.")
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Parameter(description = "이메일 주소", example = "user@example.com") @RequestParam String email,
            @Parameter(description = "비밀번호 (8자 이상)", example = "Test1234!") @RequestParam String password,
            @Parameter(description = "이름", example = "홍길동") @RequestParam String name,
            @Parameter(description = "서비스 이용약관 동의 (필수)") @RequestParam Boolean serviceAgree,
            @Parameter(description = "개인정보 처리방침 동의 (필수)") @RequestParam Boolean privacyAgree,
            @Parameter(description = "마케팅 수신 동의 (선택)") @RequestParam(required = false, defaultValue = "false") Boolean marketingAgree,
            @Parameter(description = "인증 유형 (STUDENT_ID / FACULTY_ID)", example = "STUDENT_ID") @RequestParam(required = false, defaultValue = "STUDENT_ID") String verificationType,
            @Parameter(description = "추가 메모 (선택)") @RequestParam(required = false) String note,
            @Parameter(description = "학생증·교직원증 파일 (선택)") @RequestPart(value = "verificationFile", required = false) MultipartFile verificationFile) {
        SignupRequest request = new SignupRequest(email, password, name, serviceAgree, privacyAgree, marketingAgree,
                verificationType, note);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.signup(request, verificationFile)));
    }

    @Operation(summary = "로그인", description = "이메일·비밀번호 인증 후 JWT 액세스 토큰(30분)과 리프레시 토큰(14일)을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "토큰 갱신", description = "만료된 액세스 토큰을 리프레시 토큰으로 재발급합니다. 리프레시 토큰도 함께 갱신됩니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일과 새 비밀번호를 입력해 비밀번호를 재설정합니다.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
        authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
