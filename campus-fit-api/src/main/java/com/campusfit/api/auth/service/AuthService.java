package com.campusfit.api.auth.service;

import com.campusfit.api.auth.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    SignupResponse signup(SignupRequest request, MultipartFile verificationFile);

    LoginResponse login(LoginRequest request);
}
