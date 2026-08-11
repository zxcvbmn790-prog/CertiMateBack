package com.certimate.manager.service;

import com.certimate.manager.dto.request.LoginRequest;
import com.certimate.manager.dto.request.RegisterRequest;
import com.certimate.manager.dto.request.UpdateProfileRequest;
import com.certimate.manager.dto.response.UserInfoResponse;

public interface AuthService {
    void register(RegisterRequest request);
    String login(LoginRequest request);
    String kakaoLogin(String code);
    UserInfoResponse getMe(String email);
    void updateProfile(String email, UpdateProfileRequest request);
    void withdraw(String email);
}
