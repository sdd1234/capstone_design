package com.campusfit.api.user.service;

import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.UserRepository;
import com.campusfit.api.user.dto.UpdatePasswordRequest;
import com.campusfit.api.user.dto.UpdateProfileRequest;
import com.campusfit.api.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        return UserProfileResponse.from(user);
    }

    @Override
    public UserProfileResponse update(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        user.setName(req.name());
        return UserProfileResponse.from(user);
    }

    @Override
    public void changePassword(Long userId, UpdatePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }
}
