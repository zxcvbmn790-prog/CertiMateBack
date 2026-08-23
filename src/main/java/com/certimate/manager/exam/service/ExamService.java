package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;

import java.util.List;

public interface ExamService {
    List<AiLearnResponse> generateMockExam(Long certId);
    void saveQuizHistory(String email, List<QuizHistoryItemRequest> items);
}
