package com.certimate.manager.job.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.job.dto.JobMatchingResponse;
import com.certimate.manager.job.service.JobMatchingService;
import lombok.RequiredArgsConstructor;
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
        String email = principal != null ? principal.getName() : null;
        return ApiResponse.success(jobMatchingService.getJobRecommendations(email, keyword, role, page));
    }
}
