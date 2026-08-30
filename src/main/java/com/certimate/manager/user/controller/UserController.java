package com.certimate.manager.user.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.user.dto.DashboardResponse;
import com.certimate.manager.user.dto.QuizSessionResponse;
import com.certimate.manager.exception.CustomException;
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

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @org.springframework.web.bind.annotation.DeleteMapping("/schedule")
    public ApiResponse<Void> deleteSchedule(Principal principal) {
        requireAuth(principal);
        userService.deleteSchedule(principal.getName());
        return ApiResponse.success(null);
    }

    private void requireAuth(Principal principal) {
        if (principal == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
    }
}
