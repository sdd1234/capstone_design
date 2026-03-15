package com.campusfit.api.auth.service;

import com.campusfit.api.auth.dto.*;
import com.campusfit.api.common.enums.FilePurpose;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.campusfit.api.security.JwtUtil;
import com.campusfit.api.storage.FileStorageService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final StudentVerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    @Override
    public SignupResponse signup(SignupRequest request, MultipartFile verificationFile) {
        if (userRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("이미 사용 중인 이메일입니다.");
        }
        if (!Boolean.TRUE.equals(request.serviceAgree()) || !Boolean.TRUE.equals(request.privacyAgree())) {
            throw BusinessException.badRequest("필수 약관에 동의해야 합니다.");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .marketingAgree(Boolean.TRUE.equals(request.marketingAgree()))
                .build();
        userRepository.save(user);

        StudentVerification verification = null;
        if (verificationFile != null && !verificationFile.isEmpty()) {
            String storedPath = fileStorageService.store(verificationFile);
            FileEntity fileEntity = FileEntity.builder()
                    .originalName(verificationFile.getOriginalFilename())
                    .storedPath(storedPath)
                    .mimeType(verificationFile.getContentType())
                    .size(verificationFile.getSize())
                    .purpose(FilePurpose.STUDENT_VERIFICATION)
                    .uploadedBy(user)
                    .build();
            fileRepository.save(fileEntity);

            verification = StudentVerification.builder()
                    .user(user)
                    .file(fileEntity)
                    .verificationType(request.verificationType() != null ? request.verificationType() : "STUDENT_ID")
                    .note(request.note())
                    .build();
            verificationRepository.save(verification);
        }

        return new SignupResponse(user.getId(), user.getEmail(), user.getStatus().name(),
                verification != null ? verification.getStatus().name() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> BusinessException.badRequest("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.badRequest("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new LoginResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getName(),
                user.getRole().name());
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw BusinessException.badRequest("유효하지 않은 refresh token입니다.");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw BusinessException.badRequest("refresh token이 아닙니다.");
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole().name());
        return new RefreshResponse(newAccessToken);
    }
}
