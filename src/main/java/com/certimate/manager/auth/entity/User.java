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

    @Column(name = "kakao_access_token", length = 512)
    private String kakaoAccessToken;

    @Column(name = "kakao_refresh_token", length = 512)
    private String kakaoRefreshToken;

    @Column(name = "kakao_notify_enabled")
    private Boolean kakaoNotifyEnabled;

    @Column(name = "kakao_notify_days", length = 100)
    private String kakaoNotifyDays;

    @Column(name = "kakao_notify_time", length = 10)
    private String kakaoNotifyTime;

    @Column(name = "google_id", unique = true)
    private String googleId;

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

    // 소셜 로그인 프로필 이미지 백필: 아직 이미지가 없을 때만 채운다(직접 올린 사진은 보존).
    public void applySocialProfileImage(String url) {
        if (url != null && !url.isBlank() && (this.profileImage == null || this.profileImage.isBlank())) {
            this.profileImage = url;
        }
    }

    public void updateKakaoTokens(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            this.kakaoAccessToken = accessToken;
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.kakaoRefreshToken = refreshToken;
        }
        if (this.kakaoNotifyEnabled == null) {
            this.kakaoNotifyEnabled = true;
        }
        if (this.kakaoNotifyDays == null || this.kakaoNotifyDays.isBlank()) {
            this.kakaoNotifyDays = "7,1";
        }
        if (this.kakaoNotifyTime == null || this.kakaoNotifyTime.isBlank()) {
            this.kakaoNotifyTime = "09:00";
        }
    }

    public void updateKakaoNotifyEnabled(Boolean enabled) {
        this.kakaoNotifyEnabled = enabled;
    }

    public void updateKakaoNotifySettings(Boolean enabled, String notifyDays, String notifyTime) {
        if (enabled != null) {
            this.kakaoNotifyEnabled = enabled;
        }
        if (notifyDays != null && !notifyDays.isBlank()) {
            this.kakaoNotifyDays = notifyDays;
        }
        if (notifyTime != null && !notifyTime.isBlank()) {
            this.kakaoNotifyTime = notifyTime;
        }
    }
}
