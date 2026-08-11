package com.certimate.manager.user.dto;

import java.util.List;

public record QuizSessionResponse(
        int sessionNum,
        String timeLabel,
        List<QuizHistoryItemResponse> records
) {
    public record QuizHistoryItemResponse(
            Long learnId,
            String question,
            String options,
            String userAnswer,
            String correctAnswer,
            boolean isCorrect,
            String explanation,
            String solvedAtStr
    ) {}
}
