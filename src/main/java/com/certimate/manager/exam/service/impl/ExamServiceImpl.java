package com.certimate.manager.exam.service.impl;

import com.certimate.manager.exam.entity.AiLearn;
import com.certimate.manager.exam.entity.UserQuizHistory;
import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.repository.AiLearnRepository;
import com.certimate.manager.exam.repository.UserQuizHistoryRepository;
import com.certimate.manager.exam.service.ExamService;
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
    private final com.certimate.manager.auth.repository.UserRepository userRepository;
    private final com.certimate.manager.user.repository.UserLearnLogRepository userLearnLogRepository;
    private final com.certimate.manager.user.repository.CertificationRepository certificationRepository;

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
    public void saveQuizHistory(String email, List<QuizHistoryItemRequest> items) {
        com.certimate.manager.auth.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.certimate.manager.exception.CustomException(org.springframework.http.HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Long userId = user.getId();

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
        System.out.println("DEBUG: userQuizHistory saved, items size=" + items.size());

        if (!items.isEmpty()) {
            System.out.println("DEBUG: first learnId=" + items.get(0).learnId());
            AiLearn learn = aiLearnRepository.findById(items.get(0).learnId()).orElse(null);
            System.out.println("DEBUG: AiLearn found=" + (learn != null));
            if (learn != null) {
                Long certId = learn.getCertId();
                System.out.println("DEBUG: certId=" + certId);
                com.certimate.manager.user.entity.Certification cert = certificationRepository.findById(certId).orElse(null);
                System.out.println("DEBUG: Certification found=" + (cert != null));
                if (cert != null) {
                    long correctCount = items.stream().filter(QuizHistoryItemRequest::isCorrect).count();
                    float currentCorrectRate = (float) correctCount / items.size() * 100.0f;
                    int assumedStudyMin = 30; // 프론트에서 넘어오지 않으므로 임의 30분 산정
                    System.out.println("DEBUG: calculating stats, correctRate=" + currentCorrectRate);

                    com.certimate.manager.user.entity.UserLearnLog log = userLearnLogRepository
                            .findByUser_IdAndCertification_Id(userId, certId)
                            .orElse(com.certimate.manager.user.entity.UserLearnLog.builder()
                                    .user(user)
                                    .certification(cert)
                                    .studyTimeMin(0)
                                    .correctRate(0.0f)
                                    .build());

                    log.updateStats(assumedStudyMin, currentCorrectRate);
                    userLearnLogRepository.save(log);
                    System.out.println("DEBUG: userLearnLog saved");
                }
            }
        }
    }
}
