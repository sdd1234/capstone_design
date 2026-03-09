package com.campusfit.api.auth.dto;

public record SignupResponse(
        Long userId,
        String email,
        String status,
        String verificationStatus) {
}
