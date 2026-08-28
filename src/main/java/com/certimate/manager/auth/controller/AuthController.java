package com.certimate.manager.auth.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.auth.dto.KakaoLoginRequest;
import com.certimate.manager.auth.dto.LoginRequest;
import com.certimate.manager.auth.dto.RegisterRequest;
import com.certimate.manager.auth.dto.UpdateProfileRequest;
import com.certimate.manager.auth.dto.UserInfoResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        String token = authService.login(request);
        setCookie(response, token);
        return ApiResponse.success("로그인에 성공했습니다.");
    }

    // 카카오 로그인 API 창구
    @PostMapping("/kakao")
    public ApiResponse<String> kakaoLogin(@RequestBody KakaoLoginRequest request, HttpServletResponse response) {
        // 서비스에서 카카오와 통신 후 JWT 토큰을 받아옵니다.
        String token = authService.kakaoLogin(request.code());

        // 보안 쿠키를 구워줍니다.
        setCookie(response, token);
        return ApiResponse.success("카카오 로그인에 성공했습니다.");
    }

    // 구글 로그인 API 창구 (카카오와 동일하게 code만 받는다)
    @PostMapping("/google")
    public ApiResponse<String> googleLogin(@RequestBody KakaoLoginRequest request, HttpServletResponse response) {
        String token = authService.googleLogin(request.code());
        setCookie(response, token);
        return ApiResponse.success("구글 로그인에 성공했습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getMe(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(authService.getMe(principal.getName()));
    }

    @PutMapping("/me")
    public ApiResponse<String> updateMe(Principal principal, @RequestBody UpdateProfileRequest request) {
        requireAuth(principal);
        authService.updateProfile(principal.getName(), request);
        return ApiResponse.success("프로필이 성공적으로 수정되었습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<String> deleteMe(Principal principal, HttpServletResponse response) {
        requireAuth(principal);
        authService.withdraw(principal.getName());

        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success("회원탈퇴가 완료되었습니다.");
    }

    private void requireAuth(Principal principal) {
        if (principal == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
    }

    // 중복되는 쿠키 굽기 로직을 메서드로 분리
    private void setCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false) // 로컬 테스트용. 나중에 HTTPS 배포 시 true로 변경!
                .path("/")
                .maxAge(60 * 60) // 1시간 유지
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
