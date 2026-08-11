package com.certimate.manager.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    private String major;
    private String interest;
    private String status;

    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

    @Column(name = "agree_consent", nullable = false)
    private Boolean agreeConsent;

    @Column(name = "profile_image", columnDefinition = "LONGTEXT")
    private String profileImage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateProfile(String name, String major, String interest, String status, String encodedPassword, String profileImage, Boolean agreeConsent) {
        if (name != null && !name.isBlank()) this.name = name;
        this.major = major;
        this.interest = interest;
        this.status = status;
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
        if (profileImage != null && !profileImage.isBlank()) {
            this.profileImage = profileImage;
        }
        if (agreeConsent != null) {
            this.agreeConsent = agreeConsent;
        }
    }
}
