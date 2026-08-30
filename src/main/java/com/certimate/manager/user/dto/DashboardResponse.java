package com.certimate.manager.user.dto;

import java.util.List;

public record DashboardResponse(
        int certCount,
        int scrapCount,
        List<ScrapItem> recentScraps,
        String totalStudyTime,
        String cbtAccuracy,
        List<HeatmapPoint> heatmapData,
        TargetExam targetExam,
        List<CertStat> certStats,
        List<CertTrend> certTrends,
        List<CertInfo> allCertifications
) {
    public record CertStat(Long certId, String certName, String studyTime, String accuracy) {}

    public record ScrapItem(String title, String id) {}

    public record HeatmapPoint(String date, int count) {}

    public record TargetExam(String certName, String examType, String examDate, long dDay, String achievementRate) {}

    public record CertTrend(String certName, List<TrendPoint> trendData) {}

    public record TrendPoint(String date, float accuracy) {}
    
    public record CertInfo(Long id, String name) {}
}
