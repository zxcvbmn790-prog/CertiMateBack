package com.certimate.manager.exam.service;

import com.certimate.manager.exam.dto.AiRecommendRequest;
import com.certimate.manager.exam.dto.AiRecommendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRecommendationService {

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    private final RestTemplate restTemplate;

    public AiRecommendationService() {
        this.restTemplate = new RestTemplate();
    }

    public AiRecommendResponse getRecommendation(AiRecommendRequest request) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            return new AiRecommendResponse("AI API 키가 설정되지 않았습니다. 백엔드의 .env 파일을 확인해 주세요.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        // 프롬프트 구성
        String prompt = "당신은 IT 자격증 전문 진로 상담 AI 'CertiMate'입니다.\n" +
                "사용자가 선택한 키워드: " + (request.getTags() != null ? String.join(", ", request.getTags()) : "없음") + "\n" +
                "추가 코멘트: " + (request.getCustomInput() != null ? request.getCustomInput() : "없음") + "\n\n" +
                "위 정보를 바탕으로 사용자에게 딱 맞는 IT 자격증 취득 로드맵 3가지를 1순위, 2순위, 3순위로 추천해주세요.\n" +
                "응답은 반드시 아래 JSON 배열 형식으로만 작성하고, 마크다운 코드 블록(```json)이나 다른 인사말은 절대 포함하지 마세요.\n" +
                "주의: 자격증 이름(name)은 절대 길게 쓰지 마세요. 예를 들어 'AWS Certified Solutions Architect - Associate (AWS SAA)'는 'AWS SAA'로, 'CKA (Certified Kubernetes Administrator)'는 'CKA'로 매우 짧은 약어나 대표 명칭(최대 10자 내외)으로만 적으세요.\n" +
                "[\n" +
                "  { \"rank\": 1, \"name\": \"자격증 이름(짧게)\", \"reason\": \"이 자격증을 1순위로 추천하는 구체적이고 상세한 이유\" },\n" +
                "  { \"rank\": 2, \"name\": \"자격증 이름\", \"reason\": \"상세한 추천 이유\" },\n" +
                "  { \"rank\": 3, \"name\": \"자격증 이름\", \"reason\": \"상세한 추천 이유\" }\n" +
                "]";

        // 요청 바디 생성 (Gemini API 스펙)
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", prompt);
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
            
            // 응답 파싱
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> contentResp = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> respParts = (List<Map<String, Object>>) contentResp.get("parts");
                    if (!respParts.isEmpty()) {
                        String text = (String) respParts.get(0).get("text");
                        // 마크다운 블록이 포함되어 있다면 제거
                        text = text.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("```\\s*$", "").trim();
                        return new AiRecommendResponse(text);
                    }
                }
            }
            return new AiRecommendResponse("AI 응답을 해석할 수 없습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return new AiRecommendResponse("AI 서버와의 통신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
