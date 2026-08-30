package com.certimate.manager.exam.dto;

import com.certimate.manager.user.repository.CertificationRepository.CertSummaryProjection;

// CBT 종목 선택 화면에 뿌릴 자격증 요약 (실제 등록된 문항수 포함)
public record CertSummaryResponse(
        Long certId,
        String certName,
        String difficulty,
        String agency,
        long totalQuestions,
        long subject1,
        long subject2,
        long subject3
) {
    public static CertSummaryResponse from(CertSummaryProjection p) {
        return new CertSummaryResponse(
                p.getCertId(),
                p.getCertName(),
                p.getDifficulty(),
                p.getAgency(),
                nz(p.getTotalQuestions()),
                nz(p.getSubject1()),
                nz(p.getSubject2()),
                nz(p.getSubject3())
        );
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
