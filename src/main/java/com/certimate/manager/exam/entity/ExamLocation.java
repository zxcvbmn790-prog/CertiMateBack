package com.certimate.manager.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 시험장 위치 (8천여 개, 위경도 포함). 좌표가 이미 있어 geocoding 불필요.
@Entity
@Table(name = "qnet_exam_info")
@Getter
@NoArgsConstructor
public class ExamLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qual_name", nullable = false)
    private String qualName;

    @Column(name = "exam_round")
    private String examRound;

    @Column(name = "exam_date")
    private String examDate;

    @Column(name = "test_site")
    private String testSite;

    private String address;

    // DB에 VARCHAR로 저장되어 있어 String으로 매핑 (프론트에서 지도 마커용 파싱)
    private String latitude;

    private String longitude;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;
}
