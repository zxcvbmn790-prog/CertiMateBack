package com.certimate.manager.user.entity;

import com.certimate.manager.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_scrap")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserScrap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certification certification;
}
