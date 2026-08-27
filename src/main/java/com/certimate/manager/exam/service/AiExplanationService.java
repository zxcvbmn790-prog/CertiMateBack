package com.certimate.manager.exam.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.certimate.manager.exam.entity.AiLearn;
import com.certimate.manager.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 해설이 없는 문제에 대해 Claude로 해설을 생성한다.
 * API 키가 없으면 이 기능만 실패하고 나머지 서비스는 정상 동작한다.
 */
@Slf4j
@Service
public class AiExplanationService {

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-opus-5}")
    private String model;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generate(AiLearn q) {
        if (!isEnabled()) {
            throw new CustomException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 해설 기능이 설정되지 않았습니다. (ANTHROPIC_API_KEY 필요)");
        }

        // 클라이언트는 매 요청마다 만들지 않고 재사용해도 되지만, 배치 호출 빈도가 낮아 단순화한다.
        // ponytail: 요청마다 클라이언트 생성. 호출량 늘면 @Bean 싱글턴으로 승격.
        AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();

        String prompt = """
                다음은 자격증 시험(정보처리산업기사 등) 객관식 문제입니다.
                정답이 왜 정답인지, 그리고 핵심 개념을 한국어로 간결하게 3~5문장으로 설명하세요.
                불필요한 인사말 없이 해설 본문만 작성하세요.

                문제: %s
                선택지(JSON): %s
                정답 번호: %s
                """.formatted(q.getQuestion(), q.getOptions(), q.getAnswer());

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(1024L)
                    .addUserMessage(prompt)
                    .build();

            Message response = client.messages().create(params);

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(t -> t.text())
                    .reduce("", String::concat)
                    .trim();

            if (text.isBlank()) {
                throw new CustomException(HttpStatus.BAD_GATEWAY, "AI가 빈 해설을 반환했습니다.");
            }
            return text;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 해설 생성 실패 (learnId={})", q.getLearnId(), e);
            throw new CustomException(HttpStatus.BAD_GATEWAY, "AI 해설 생성에 실패했습니다.");
        }
    }
}
