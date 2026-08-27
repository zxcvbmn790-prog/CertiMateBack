package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.dto.CertSummaryResponse;

import java.util.List;

public interface ExamService {
    List<CertSummaryResponse> listCertifications();
    List<AiLearnResponse> generateMockExam(Long certId);
    void saveQuizHistory(Long userId, List<QuizHistoryItemRequest> items);
}
