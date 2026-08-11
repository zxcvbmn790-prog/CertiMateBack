package com.certimate.manager.service;

import com.certimate.manager.dto.request.QuizHistoryItemRequest;
import com.certimate.manager.dto.response.AiLearnResponse;

import java.util.List;

public interface ExamService {
    List<AiLearnResponse> generateMockExam(Long certId);
    void saveQuizHistory(Long userId, List<QuizHistoryItemRequest> items);
}
