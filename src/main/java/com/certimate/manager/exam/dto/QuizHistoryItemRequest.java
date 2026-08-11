package com.certimate.manager.exam.dto;

public record QuizHistoryItemRequest(
        Long learnId,
        String userAnswer,
        Boolean isCorrect
) {
}
