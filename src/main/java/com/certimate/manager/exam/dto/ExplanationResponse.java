package com.certimate.manager.exam.dto;

// 채점 후 생성된 AI 해설 (learnId 기준으로 프론트가 병합)
public record ExplanationResponse(
        Long learnId,
        String explanation
) {}
