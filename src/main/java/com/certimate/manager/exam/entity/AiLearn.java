package com.certimate.manager.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_learn")
@Getter
@NoArgsConstructor
public class AiLearn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long learnId;

    @Column(name = "cert_id", nullable = false)
    private Long certId;

    @Column(name = "subject_num", nullable = false)
    private Integer subjectNum;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "JSON", nullable = false)
    private String options;

    @Column(nullable = false)
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // true면 AI가 생성한 해설(오류 가능, 신고 대상), false면 사람이 작성한 해설(신뢰)
    @Column(name = "explanation_ai", nullable = false)
    private boolean explanationAi;

    // AI가 생성한 해설을 채워 넣는다 (최초 1회 생성 후 DB에 영구 저장, AI 플래그 표시)
    public void applyExplanation(String generated) {
        this.explanation = generated;
        this.explanationAi = true;
    }

    // 신고된 AI 해설 제거 → 다음에 다시 요청하면 재생성된다
    public void clearExplanation() {
        this.explanation = null;
        this.explanationAi = false;
    }
}
