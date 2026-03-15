package com.campusfit.api.user.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.user.dto.UpdatePasswordRequest;
import com.campusfit.api.user.dto.UpdateProfileRequest;
import com.campusfit.api.user.dto.UserProfileResponse;
import com.campusfit.api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 프로필", description = "내 정보 조회·수정·비밀번호 변경 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 이름·이메일·역할·인증 상태를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.me(userId)));
    }

    @Operation(summary = "내 정보 수정", description = "이름과 마케팅 수신 동의 여부를 변경합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(
            @Valid @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.update(userId, req)));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UpdatePasswordRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        userService.changePassword(userId, req);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
