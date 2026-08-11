package com.certimate.manager.domain.entity;

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
}
