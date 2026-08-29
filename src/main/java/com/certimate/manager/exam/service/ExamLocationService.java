package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.ExamLocationResponse;
import com.certimate.manager.exam.dto.GlobalScheduleResponse;

import com.certimate.manager.exam.repository.ExamLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamLocationService {

    private final ExamLocationRepository examLocationRepository;

    public List<ExamLocationResponse> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        return examLocationRepository.search(query.trim()).stream()
                .map(ExamLocationResponse::of)
                .collect(Collectors.toList());
    }

    public List<ExamLocationResponse> nearest(double lat, double lng, int limit) {
        int n = Math.max(1, Math.min(limit, 50)); // 1~50 제한
        return examLocationRepository.findNearest(lat, lng, n).stream()
                .map(p -> new ExamLocationResponse(
                        p.getId(), p.getQualName(), p.getExamRound(), p.getExamDate(), p.getTestSite(), p.getAddress(),
                        p.getLatitude(), p.getLongitude(),
                        p.getDistanceKm() == null ? null : Math.round(p.getDistanceKm() * 10.0) / 10.0))
                .collect(Collectors.toList());
    }
    public List<GlobalScheduleResponse> findAllSchedules() {
        return examLocationRepository.findAllSchedules().stream()
                .map(GlobalScheduleResponse::of)
                .toList();
    }
}