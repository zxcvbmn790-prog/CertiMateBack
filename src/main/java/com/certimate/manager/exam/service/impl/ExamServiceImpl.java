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
    private final com.certimate.manager.auth.repository.UserRepository userRepository;
    private final com.certimate.manager.user.repository.UserLearnLogRepository userLearnLogRepository;
    private final com.certimate.manager.user.repository.CertificationRepository certificationRepository;
    private final AiExplanationService aiExplanationService;

    @Override
    public List<CertSummaryResponse> listCertifications() {
        return certificationRepository.findCertificationsWithQuestions().stream()
                .map(p -> CertSummaryResponse.from(p, java.util.Arrays.stream(
                        SUBJECT_QUESTION_COUNTS.getOrDefault(p.getCertName(), DEFAULT_SUBJECT_QUESTION_COUNTS)).sum()))
                .collect(java.util.stream.Collectors.toList());
    }

    // 실제 필기시험의 과목별 문항 수. 등록되지 않은 자격증은 1~3과목 × 20문제로 출제한다.
    // ponytail: 자격증명으로 찾는 정적 표. 종목이 늘어 관리가 번거로워지면 certification 테이블 컬럼으로 옮길 것.
    private static final java.util.Map<String, int[]> SUBJECT_QUESTION_COUNTS = java.util.Map.of(
            "정보처리기사", new int[]{20, 20, 20, 20, 20},
            "정보처리산업기사", new int[]{20, 20, 20},
            "SQL 개발자 (SQLD)", new int[]{10, 40},
            "데이터분석준전문가 (ADsP)", new int[]{10, 10, 30}
    );
    private static final int[] DEFAULT_SUBJECT_QUESTION_COUNTS = {20, 20, 20};

    @Override
    public List<AiLearnResponse> generateMockExam(Long certId) {
        String certName = certificationRepository.findById(certId).map(c -> c.getCertName()).orElse("");
        int[] counts = SUBJECT_QUESTION_COUNTS.getOrDefault(certName, DEFAULT_SUBJECT_QUESTION_COUNTS);

        List<AiLearn> mockExam = new ArrayList<>();
        // 과목 순서대로, 과목마다 문제은행에서 무작위로 정해진 수만큼 뽑는다.
        for (int subjectNum = 1; subjectNum <= counts.length; subjectNum++) {
            List<AiLearn> allQuestions = new ArrayList<>(aiLearnRepository.findByCertIdAndSubjectNum(certId, subjectNum));
            Collections.shuffle(allQuestions);
            mockExam.addAll(allQuestions.subList(0, Math.min(allQuestions.size(), counts[subjectNum - 1])));
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
