package com.certimate.manager.auth.service;

import com.certimate.manager.auth.dto.LoginRequest;
import com.certimate.manager.auth.dto.RegisterRequest;
import com.certimate.manager.auth.dto.UpdateProfileRequest;
import com.certimate.manager.auth.dto.UserInfoResponse;

public interface AuthService {
    void register(RegisterRequest request);
    String login(LoginRequest request);
    String kakaoLogin(String code);
    UserInfoResponse getMe(String email);
    void updateProfile(String email, UpdateProfileRequest request);
    void withdraw(String email);
}
