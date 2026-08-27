package com.certimate.manager.user.entity;

import com.certimate.manager.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_certification")
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
