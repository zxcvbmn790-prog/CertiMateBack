package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.AiCoverLetterRequest;
import com.certimate.manager.exam.dto.AiCoverLetterResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiCoverLetterService {

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    @Value("classpath:prompts/cover_letter_prompt.txt")
    private Resource promptResource;

    private final RestTemplate restTemplate;

    public AiCoverLetterService() {
        this.restTemplate = new RestTemplate();
    }

    public AiCoverLetterResponse generateCoverLetter(AiCoverLetterRequest request) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            return new AiCoverLetterResponse("AI API 키가 설정되지 않았습니다. 백엔드의 .env 파일을 확인해주세요.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        String basePrompt = "";
        try {
            basePrompt = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new AiCoverLetterResponse("프롬프트 파일을 읽을 수 없습니다: " + e.getMessage());
        }

        // 사용자 입력 추가
        StringBuilder promptBuilder = new StringBuilder(basePrompt);
        promptBuilder.append("\n\n[사용자 입력 정보]\n");
        promptBuilder.append("- 지원 기업명: ").append(request.getCompanyName() != null ? request.getCompanyName() : "없음").append("\n");
        promptBuilder.append("- 지원 직무: ").append(request.getJobRole() != null ? request.getJobRole() : "없음").append("\n");
        
        promptBuilder.append("- 선택한 자기소개서 항목: ");
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            promptBuilder.append(String.join(", ", request.getTags()));
        } else {
            promptBuilder.append("없음");
        }
        promptBuilder.append("\n");

        promptBuilder.append("- 장점: ").append(request.getPros() != null && !request.getPros().isEmpty() ? request.getPros() : "없음").append("\n");
        promptBuilder.append("- 단점: ").append(request.getCons() != null && !request.getCons().isEmpty() ? request.getCons() : "없음").append("\n");

        promptBuilder.append("- 사용자 경험 목록:\n");
        if (request.getExperiences() != null && !request.getExperiences().isEmpty()) {
            for (int i = 0; i < request.getExperiences().size(); i++) {
                promptBuilder.append("  ").append(i + 1).append(". ").append(request.getExperiences().get(i)).append("\n");
            }
        } else {
            promptBuilder.append("  없음\n");
        }

        if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isEmpty()) {
            promptBuilder.append("\n[사용자 추가 피드백 / 요청사항]\n");
            promptBuilder.append("이전 피드백에 대해 사용자가 다음과 같이 추가 정보를 제공했거나 수정을 요청했습니다:\n");
            promptBuilder.append(request.getAdditionalInfo()).append("\n");
            promptBuilder.append("위 추가 요청사항을 최우선으로 반영하여 다시 작성해주세요.\n");
        }

        if (request.isFinal()) {
            promptBuilder.append("\n[최종 자소서 작성 명령]\n");
            promptBuilder.append("이 요청은 사용자가 '자소서 완성하기' 버튼을 눌렀을 때 보내집니다.\n");
            promptBuilder.append("지금까지 논의된 초안과 피드백 내용을 바탕으로, 완벽하고 전문적인 최종 자기소개서 완성본을 작성해주세요.\n");
            promptBuilder.append("피드백이나 부가적인 설명(예: '이렇게 작성했습니다' 등)은 절대 포함하지 말고, 오직 자기소개서 본문 텍스트만 출력해주세요.\n");
        }

        String finalPrompt = promptBuilder.toString();

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", finalPrompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> contentResp = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> respParts = (List<Map<String, Object>>) contentResp.get("parts");
                    if (!respParts.isEmpty()) {
                        String text = (String) respParts.get(0).get("text");
                        return new AiCoverLetterResponse(text.trim());
                    }
                }
            }
            return new AiCoverLetterResponse("AI 응답을 해석할 수 없습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return new AiCoverLetterResponse("AI 서버와 통신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
