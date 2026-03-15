package com.campusfit.api.user.dto;

import com.campusfit.api.domain.User;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String status,
        String role,
        Boolean marketingAgree,
        LocalDateTime createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus().name(),
                user.getRole().name(),
                user.getMarketingAgree(),
                user.getCreatedAt());
    }
}
