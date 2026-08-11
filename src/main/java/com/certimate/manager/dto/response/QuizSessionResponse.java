package com.certimate.manager.dto.response;

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
