package com.certimate.manager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팀원들이 실제 도메인 엔티티를 추가할 때 참고할 예시입니다.
 * 실제 개발 시작 시 이 파일과 관련 Repository/Service/Controller/DTO는 삭제하고
 * 같은 패턴으로 도메인 엔티티(Certificate, Domain 등)를 만들어 주세요.
 */
@Entity
@Table(name = "example")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Example {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Example(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
    }
}
