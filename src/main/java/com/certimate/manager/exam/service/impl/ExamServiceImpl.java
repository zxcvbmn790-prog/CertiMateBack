package com.certimate.manager.exam.service.impl;

import com.certimate.manager.exam.entity.AiLearn;
import com.certimate.manager.exam.entity.UserQuizHistory;
import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.dto.CertSummaryResponse;
import com.certimate.manager.exam.dto.ExplanationResponse;
import com.certimate.manager.exam.repository.AiLearnRepository;
import com.certimate.manager.exam.repository.UserQuizHistoryRepository;
import com.certimate.manager.exam.service.AiExplanationService;
import com.certimate.manager.exam.service.ExamService;
import com.certimate.manager.user.repository.CertificationRepository;
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
    private final CertificationRepository certificationRepository;
    private final AiExplanationService aiExplanationService;

    @Override
    public List<CertSummaryResponse> listCertifications() {
        return certificationRepository.findCertificationsWithQuestions().stream()
                .map(CertSummaryResponse::from)
                .collect(Collectors.toList());
    }

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

    // 채점 시 해설이 비어있는 문제들에 대해 AI 해설을 일괄 생성하고 DB에 저장한다.
    // 이미 해설이 있으면 건너뛰므로 한 문제당 생성은 평생 최대 1회.
    // ponytail: 문제별 순차 호출. 한 번에 다수 결측이면 느릴 수 있음 — 필요 시 프롬프트 배치로 묶을 것.
    @Override
    @Transactional
    public List<ExplanationResponse> generateExplanations(List<Long> learnIds) {
        List<ExplanationResponse> results = new ArrayList<>();
        if (learnIds == null || learnIds.isEmpty()) return results;

        for (Long learnId : learnIds) {
            AiLearn q = aiLearnRepository.findById(learnId).orElse(null);
            if (q == null) continue;

            // 이미 해설이 있으면 그대로 반환 (재생성/재과금 방지)
            if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                results.add(new ExplanationResponse(learnId, q.getExplanation(), q.isExplanationAi()));
                continue;
            }

            String generated = aiExplanationService.generate(q);
            q.applyExplanation(generated);
            aiLearnRepository.save(q);
            results.add(new ExplanationResponse(learnId, generated, true));
        }
        return results;
    }

    // AI 해설 신고: 해설을 제거해 즉시 숨기고, 다음에 다시 요청하면 재생성되게 한다.
    // 사람이 작성한 해설(explanationAi=false)은 신고 대상이 아니므로 건드리지 않는다.
    @Override
    @Transactional
    public void reportExplanation(Long learnId) {
        AiLearn q = aiLearnRepository.findById(learnId).orElse(null);
        if (q == null || !q.isExplanationAi()) return;
        q.clearExplanation();
        aiLearnRepository.save(q);
    }
}
