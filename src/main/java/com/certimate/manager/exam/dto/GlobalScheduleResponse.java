package com.certimate.manager.exam.dto;

import com.certimate.manager.exam.repository.ExamLocationRepository.ScheduleProjection;

public record GlobalScheduleResponse(
        String qualName,
        String examRound,
        String examDate,
        String examType
) {
    public static GlobalScheduleResponse of(ScheduleProjection p) {
        return new GlobalScheduleResponse(p.getQualName(), p.getExamRound(), p.getExamDate(), p.getExamType());
    }
}
