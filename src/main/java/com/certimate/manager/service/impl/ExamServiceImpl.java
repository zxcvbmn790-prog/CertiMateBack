package com.certimate.manager.service.impl;

import com.certimate.manager.domain.entity.AiLearn;
import com.certimate.manager.domain.entity.UserQuizHistory;
import com.certimate.manager.dto.request.QuizHistoryItemRequest;
import com.certimate.manager.dto.response.AiLearnResponse;
import com.certimate.manager.repository.AiLearnRepository;
import com.certimate.manager.repository.UserQuizHistoryRepository;
import com.certimate.manager.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final AiLearnRepository aiLearnRepository;
    private final UserQuizHistoryRepository userQuizHistoryRepository;

    @Override
    public List<AiLearnResponse> generateMockExam(Long certId) {
        List<AiLearn> mockExam = new ArrayList<>();

        // 1과목, 2과목, 3과목 순서대로 반복
        for (int subjectNum = 1; subjectNum <= 3; subjectNum++) {
            // 1. 해당 자격증(certId)과 과목(subjectNum)의 전체 문제 가져오기
            List<AiLearn> allQuestions = new ArrayList<>(aiLearnRepository.findByCertIdAndSubjectNum(certId, subjectNum));

            // 2. 전체 문제를 무작위로 섞기
            Collections.shuffle(allQuestions);

            // 3. 앞에서부터 딱 20개만 잘라서 모의고사 리스트에 추가
            int limit = Math.min(allQuestions.size(), 20);
            mockExam.addAll(allQuestions.subList(0, limit));
        }

        return mockExam.stream().map(AiLearnResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveQuizHistory(Long userId, List<QuizHistoryItemRequest> items) {
        List<UserQuizHistory> histories = items.stream()
                .map(item -> UserQuizHistory.builder()
                        .userId(userId) // 핵심: DB 필수값이므로 반드시 세팅
                        .learnId(item.learnId())
                        .userAnswer(item.userAnswer())
                        .isCorrect(item.isCorrect())
                        .build())
                .collect(Collectors.toList());

        // 여러 건의 데이터를 한 번에 저장 (배치 인서트 효과)
        userQuizHistoryRepository.saveAll(histories);
    }
}
