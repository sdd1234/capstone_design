package com.campusfit.api.auth.controller;

import com.campusfit.api.auth.dto.LoginRequest;
import com.campusfit.api.auth.dto.SignupRequest;
import com.campusfit.api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(
        @Valid @RequestPart("payload") SignupRequest request,
        @RequestPart("verificationFile") MultipartFile verificationFile
    ) {
        Map<String, Object> data = Map.of(
            "email", request.email(),
            "status", "PENDING_VERIFICATION",
            "verificationStatus", "PENDING",
            "fileName", verificationFile.getOriginalFilename()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, String> tokens = Map.of(
            "accessToken", "mock-access-token",
            "refreshToken", "mock-refresh-token"
        );
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }
}
