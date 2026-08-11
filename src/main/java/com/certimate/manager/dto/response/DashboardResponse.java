package com.certimate.manager.dto.response;

import java.util.List;

public record DashboardResponse(
        int certCount,
        int scrapCount,
        List<ScrapItem> recentScraps,
        String totalStudyTime,
        String cbtAccuracy,
        List<HeatmapPoint> heatmapData,
        TargetExam targetExam
) {
    public record ScrapItem(String title, String id) {}

    public record HeatmapPoint(String date, int count) {}

    public record TargetExam(String certName, String examType, String examDate, long dDay, String achievementRate) {}
}
