package com.certimate.manager.user.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.user.dto.DashboardResponse;
import com.certimate.manager.user.dto.QuizSessionResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.user.service.KakaoNotificationService;
import com.certimate.manager.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoNotificationService kakaoNotificationService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> getDashboard(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(userService.getDashboard(principal.getName()));
    }

    @GetMapping("/quiz-history")
    public ApiResponse<List<QuizSessionResponse>> getQuizHistory(@RequestParam String date, Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(userService.getQuizHistory(principal.getName(), date));
    }

    @PostMapping("/schedule")
    public ApiResponse<Void> addSchedule(@RequestBody com.certimate.manager.user.dto.AddScheduleRequest request, Principal principal) {
        requireAuth(principal);
        userService.addSchedule(principal.getName(), request);
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/schedule/{id}")
    public ApiResponse<Void> deleteSchedule(@org.springframework.web.bind.annotation.PathVariable Long id, Principal principal) {
        requireAuth(principal);
        userService.deleteSchedule(principal.getName(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/certifications")
    public ApiResponse<Void> addAcquiredCertification(@RequestBody com.certimate.manager.user.dto.AddAcquiredCertRequest request, Principal principal) {
        requireAuth(principal);
        userService.addAcquiredCertification(principal.getName(), request);
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/certifications/{id}")
    public ApiResponse<Void> deleteAcquiredCertification(@org.springframework.web.bind.annotation.PathVariable Long id, Principal principal) {
        requireAuth(principal);
        userService.deleteAcquiredCertification(principal.getName(), id);
        return ApiResponse.success(null);
    }

    @GetMapping("/kakao-notification/status")
    public ApiResponse<Map<String, Object>> getKakaoNotificationStatus(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(kakaoNotificationService.getStatus(principal.getName()));
    }

    @PostMapping("/kakao-notification/connect")
    public ApiResponse<Void> connectKakaoNotification(@RequestBody Map<String, String> body, Principal principal) {
        requireAuth(principal);
        kakaoNotificationService.connectKakao(
                principal.getName(),
                body.get("code"),
                body.get("notifyDays"),
                body.get("notifyTime")
        );
        return ApiResponse.success(null);
    }

    @PostMapping("/kakao-notification/settings")
    public ApiResponse<Void> updateKakaoNotificationSettings(@RequestBody Map<String, Object> body, Principal principal) {
        requireAuth(principal);
        Boolean enabled = body.get("enabled") != null ? Boolean.valueOf(body.get("enabled").toString()) : null;
        String notifyDays = body.get("notifyDays") != null ? body.get("notifyDays").toString() : null;
        String notifyTime = body.get("notifyTime") != null ? body.get("notifyTime").toString() : null;
        kakaoNotificationService.updateSettings(principal.getName(), enabled, notifyDays, notifyTime);
        return ApiResponse.success(null);
    }

    private void requireAuth(Principal principal) {
        if (principal == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
    }
}
