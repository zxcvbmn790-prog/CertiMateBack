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

    // AI가 생성한 해설을 채워 넣는다 (최초 1회 생성 후 DB에 영구 저장)
    public void applyExplanation(String generated) {
        this.explanation = generated;
    }
}
