package com.certimate.manager.exam.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.exam.dto.ExamLocationResponse;
import com.certimate.manager.exam.dto.GlobalScheduleResponse;
import com.certimate.manager.exam.service.ExamLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/exams/** 는 이미 permitAll 이라 별도 보안설정 불필요
@RestController
@RequestMapping("/api/exams/locations")
@RequiredArgsConstructor
public class ExamLocationController {

    private final ExamLocationService examLocationService;

    // 시험장 검색 (이름/주소)
    @GetMapping
    public ApiResponse<List<ExamLocationResponse>> search(@RequestParam String query) {
        return ApiResponse.success(examLocationService.search(query));
    }

    // 내 주변 가까운 시험장 (거리순)
    @GetMapping("/near")
    public ApiResponse<List<ExamLocationResponse>> near(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(examLocationService.nearest(lat, lng, limit));
    }

    @GetMapping("/all-schedules")
    public ApiResponse<List<GlobalScheduleResponse>> allSchedules() {
        return ApiResponse.success(examLocationService.findAllSchedules());
    }
}