package com.certimate.manager.dto.request;

public record QuizHistoryItemRequest(
        Long learnId,
        String userAnswer,
        Boolean isCorrect
) {
}
