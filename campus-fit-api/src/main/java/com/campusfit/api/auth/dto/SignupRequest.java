package com.campusfit.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Email @Size(min = 5, max = 254) String email,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(min = 2, max = 20) String name,
    @NotNull Boolean serviceAgree,
    @NotNull Boolean privacyAgree,
    Boolean marketingAgree,
    @NotBlank String verificationType,
    String note
) {
}
