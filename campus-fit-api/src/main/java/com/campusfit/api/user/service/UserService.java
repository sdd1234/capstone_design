package com.campusfit.api.user.service;

import com.campusfit.api.user.dto.UpdatePasswordRequest;
import com.campusfit.api.user.dto.UpdateProfileRequest;
import com.campusfit.api.user.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse me(Long userId);

    UserProfileResponse update(Long userId, UpdateProfileRequest req);

    void changePassword(Long userId, UpdatePasswordRequest req);
}
