package com.certimate.manager.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "USER_CERTIFICATION")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserCertification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_cert_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certification certification;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;
}
