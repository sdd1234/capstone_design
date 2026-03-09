package com.campusfit.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(min = 5, max = 254) String email,
    @NotBlank @Size(min = 8, max = 64) String password
) {
}
