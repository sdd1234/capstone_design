package com.campusfit.api.auth.controller;

import com.campusfit.api.auth.dto.*;
import com.campusfit.api.auth.service.AuthService;
import com.campusfit.api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam Boolean serviceAgree,
            @RequestParam Boolean privacyAgree,
            @RequestParam(required = false, defaultValue = "false") Boolean marketingAgree,
            @RequestParam(required = false, defaultValue = "STUDENT_ID") String verificationType,
            @RequestParam(required = false) String note,
            @RequestPart(value = "verificationFile", required = false) MultipartFile verificationFile) {
        SignupRequest request = new SignupRequest(email, password, name, serviceAgree, privacyAgree, marketingAgree,
                verificationType, note);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.signup(request, verificationFile)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }
}
