package com.certimate.manager.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CERTIFICATION")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Certification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cert_id")
    private Long id;

    @Column(name = "cert_name", nullable = false, length = 100)
    private String certName;

    @Column(length = 50)
    private String difficulty;

    @Column(length = 100)
    private String agency;

    @Column(columnDefinition = "int default 0")
    private Integer views;
}
