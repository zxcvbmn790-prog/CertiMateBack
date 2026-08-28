package com.certimate.manager.exam.dto;

import com.certimate.manager.exam.entity.AiLearn;

// 클라이언트가 채점을 즉시 수행하므로 answer/explanation을 그대로 포함한다 (old AiLearn 노출과 동일).
public record AiLearnResponse(
        Long learnId,
        Long certId,
        Integer subjectNum,
        String question,
        String options,
        String answer,
        String explanation,
        boolean explanationAi
) {
    public static AiLearnResponse from(AiLearn aiLearn) {
        return new AiLearnResponse(
                aiLearn.getLearnId(),
                aiLearn.getCertId(),
                aiLearn.getSubjectNum(),
                aiLearn.getQuestion(),
                aiLearn.getOptions(),
                aiLearn.getAnswer(),
                aiLearn.getExplanation(),
                aiLearn.isExplanationAi()
        );
    }
}
