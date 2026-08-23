package com.certimate.manager.user.entity;

import com.certimate.manager.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_LEARN_LOG")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserLearnLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certification certification;

    @Column(name = "study_time_min", columnDefinition = "int default 0")
    private Integer studyTimeMin;

    @Column(name = "correct_rate", columnDefinition = "float default 0.0")
    private Float correctRate;

    @UpdateTimestamp
    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    public void updateStats(int addTimeMin, float newCorrectRate) {
        this.studyTimeMin = (this.studyTimeMin == null ? 0 : this.studyTimeMin) + addTimeMin;
        if (this.correctRate == null || this.correctRate == 0.0f) {
            this.correctRate = newCorrectRate;
        } else {
            this.correctRate = (this.correctRate + newCorrectRate) / 2.0f;
        }
    }
}
