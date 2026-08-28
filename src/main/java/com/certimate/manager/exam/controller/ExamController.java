package com.certimate.manager.exam.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.dto.CertSummaryResponse;
import com.certimate.manager.exam.dto.ExplanationResponse;
import com.certimate.manager.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // CBT 종목 목록 (실제 문제가 등록된 자격증만, 과목별 문항수 포함)
    @GetMapping("/certs")
    public ApiResponse<List<CertSummaryResponse>> listCerts() {
        return ApiResponse.success(examService.listCertifications());
    }

    // CBT 모의고사 출제
    @GetMapping("/{certId}/mock")
    public ApiResponse<List<AiLearnResponse>> getMockExam(@PathVariable Long certId) {
        return ApiResponse.success(examService.generateMockExam(certId));
    }

    // 해설이 없는 문제의 AI 해설 생성 (learnId 배열 — 온디맨드는 단건 배열로 호출)
    @PostMapping("/explanations")
    public ApiResponse<List<ExplanationResponse>> generateExplanations(@RequestBody List<Long> learnIds) {
        return ApiResponse.success(examService.generateExplanations(learnIds));
    }

    // AI 해설 신고 → 즉시 숨김(제거), 다음에 다시 요청하면 재생성
    @PostMapping("/{learnId}/report-explanation")
    public ApiResponse<String> reportExplanation(@PathVariable Long learnId) {
        examService.reportExplanation(learnId);
        return ApiResponse.success("신고가 접수되어 해당 해설을 숨겼습니다.");
    }

    // 모의고사 결과 저장 (오답노트용)
    // 실제 서비스에서는 인증된 사용자 ID를 사용해야 하지만, old 프로젝트와 동일하게
    // 로그인 연동 전까지는 임시로 userId 1을 고정값으로 사용한다.
    @PostMapping("/save-history")
    public ApiResponse<String> saveHistory(@RequestBody List<QuizHistoryItemRequest> historyPayload) {
        Long currentUserId = 1L;
        examService.saveQuizHistory(currentUserId, historyPayload);
        return ApiResponse.success("오답노트가 성공적으로 저장되었습니다.");
    }

    // old MockExamController에도 있던 동일 기능의 저장 엔드포인트 (호환성을 위해 유지)
    @PostMapping("/history")
    public ApiResponse<String> saveExamHistory(@RequestBody List<QuizHistoryItemRequest> historyList) {
        Long userId = 1L;
        examService.saveQuizHistory(userId, historyList);
        return ApiResponse.success("오답노트 저장 완료");
    }
}
