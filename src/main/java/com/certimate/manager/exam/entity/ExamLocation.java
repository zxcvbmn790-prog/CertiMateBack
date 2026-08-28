package com.certimate.manager.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 시험장 위치 (8천여 개, 위경도 포함). 좌표가 이미 있어 geocoding 불필요.
@Entity
@Table(name = "exam_location")
@Getter
@NoArgsConstructor
public class ExamLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_site")
    private String testSite;

    private String address;

    // DB에 VARCHAR로 저장되어 있어 String으로 매핑 (프론트에서 지도 마커용 파싱)
    private String latitude;

    private String longitude;
}
