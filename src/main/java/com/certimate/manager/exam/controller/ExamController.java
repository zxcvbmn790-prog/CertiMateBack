package com.certimate.manager.exam.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.exam.dto.QuizHistoryItemRequest;
import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // CBT 모의고사 출제
    @GetMapping("/{certId}/mock")
    public ApiResponse<List<AiLearnResponse>> getMockExam(@PathVariable Long certId) {
        return ApiResponse.success(examService.generateMockExam(certId));
    }

    // 한 문제씩 풀기(무한 학습) 모드: 랜덤 1문제 출제
    // excludeIds: 직전에 풀었던 learnId들을 쉼표로 구분해 넘기면 연속 중복 출제를 피한다
    @GetMapping("/{certId}/practice")
    public ApiResponse<AiLearnResponse> getPracticeQuestion(
            @PathVariable Long certId,
            @RequestParam(required = false) String excludeIds) {
        List<Long> excluded = (excludeIds == null || excludeIds.isBlank())
                ? List.of()
                : Arrays.stream(excludeIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();

        return ApiResponse.success(examService.getPracticeQuestion(certId, excluded));
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
