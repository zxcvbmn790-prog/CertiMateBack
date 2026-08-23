package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;

import java.util.List;

public interface ExamService {
    List<AiLearnResponse> generateMockExam(Long certId);
    void saveQuizHistory(String email, List<QuizHistoryItemRequest> items);

    // 한 문제씩 풀기(무한 학습) 모드: 과목 구분 없이 랜덤 1문제 반환 (없으면 null)
    AiLearnResponse getPracticeQuestion(Long certId, List<Long> excludeIds);
}
