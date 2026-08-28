package com.certimate.manager.exam.dto;

import com.certimate.manager.exam.entity.ExamLocation;

// 시험장 응답. distanceKm는 "내 주변" 조회일 때만 채워지고, 검색 결과에선 null.
public record ExamLocationResponse(
        Long id,
        String testSite,
        String address,
        String latitude,
        String longitude,
        Double distanceKm
) {
    public static ExamLocationResponse of(ExamLocation e) {
        return new ExamLocationResponse(e.getId(), e.getTestSite(), e.getAddress(),
                e.getLatitude(), e.getLongitude(), null);
    }
}
