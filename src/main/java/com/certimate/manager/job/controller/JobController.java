package com.certimate.manager.job.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.job.dto.JobMatchingResponse;
import com.certimate.manager.job.service.JobMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobMatchingService jobMatchingService;

    @GetMapping("/recommendations")
    public ApiResponse<JobMatchingResponse> getRecommendations(
            @RequestParam(required = false, defaultValue = "AUTO") String keyword,
            @RequestParam(required = false, defaultValue = "전체") String role,
            @RequestParam(required = false, defaultValue = "1") int page,
            Principal principal
    ) {
        // 호출마다 사람인 페이지를 조회하므로 비로그인 호출(봇·스캐너)로 반복 조회되지 않게 막는다.
        // (/api/jobs/** 는 SecurityConfig에서 permitAll이라 여기서 직접 확인한다)
        if (principal == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ApiResponse.success(jobMatchingService.getJobRecommendations(principal.getName(), keyword, role, page));
    }
}
