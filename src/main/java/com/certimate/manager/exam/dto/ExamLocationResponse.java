package com.certimate.manager.exam.dto;

import com.certimate.manager.exam.entity.ExamLocation;

// 시험장 응답. distanceKm는 "내 주변" 조회일 때만 채워지고, 검색 결과에선 null.
public record ExamLocationResponse(
        Long id,
        String qualName,
        String examRound,
        String examDate,
        String testSite,
        String address,
        String latitude,
        String longitude,
        Double distanceKm
) {
    public static ExamLocationResponse of(ExamLocation e) {
        return new ExamLocationResponse(e.getId(), e.getQualName(), e.getExamRound(), e.getExamDate(), e.getTestSite(), e.getAddress(),
                e.getLatitude(), e.getLongitude(), null);
    }
}
