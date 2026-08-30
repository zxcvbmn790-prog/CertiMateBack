package com.certimate.manager.exam.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.dto.CertSummaryResponse;
import com.certimate.manager.exam.dto.ExplanationResponse;
import com.certimate.manager.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.certimate.manager.user.repository.CertificationRepository;
import com.certimate.manager.user.entity.ExamSchedule;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final ExamScheduleRepository examScheduleRepository;
    private final CertificationRepository certificationRepository;

    // CBT 종목 목록 (실제 문제가 등록된 자격증만, 과목별 문항수 포함)

    @GetMapping("/{certId}/schedules")
    public ApiResponse<List<Map<String, Object>>> getGlobalSchedules(@PathVariable Long certId) {
        String certName = certificationRepository.findById(certId).map(com.certimate.manager.user.entity.Certification::getCertName).orElse("");
        List<ExamSchedule> schedules = examScheduleRepository.findByQualNameAndUserIsNullAndExamDateAfterOrderByExamDateAsc(certName, LocalDate.now().minusDays(1));
        List<Map<String, Object>> response = schedules.stream().map(s -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("examDate", s.getExamDate() != null ? s.getExamDate().toString() : "");
            map.put("examType", s.getExamType() != null ? s.getExamType() : "");
            map.put("examRound", s.getExamRound() != null ? s.getExamRound() : "");
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.success(response);
    }

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
    @PostMapping("/save-history")
    public ApiResponse<String> saveHistory(@RequestBody List<QuizHistoryItemRequest> historyPayload, java.security.Principal principal) {
        if (principal == null) {
            throw new com.certimate.manager.exception.CustomException(org.springframework.http.HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        examService.saveQuizHistory(principal.getName(), historyPayload);
        return ApiResponse.success("오답노트가 성공적으로 저장되었습니다.");
    }

    // old MockExamController에도 있던 동일 기능의 저장 엔드포인트 (호환성을 위해 유지)
    @PostMapping("/history")
    public ApiResponse<String> saveExamHistory(@RequestBody List<QuizHistoryItemRequest> historyList, java.security.Principal principal) {
        if (principal == null) {
            throw new com.certimate.manager.exception.CustomException(org.springframework.http.HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        examService.saveQuizHistory(principal.getName(), historyList);
        return ApiResponse.success("오답노트 저장 완료");
    }
}
