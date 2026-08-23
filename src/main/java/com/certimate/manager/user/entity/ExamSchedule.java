package com.certimate.manager.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "EXAM_SCHEDULE")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.certimate.manager.auth.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certification certification;

    @Column(name = "exam_type", nullable = false)
    private String examType;
    
    @Column(name = "target_read_count", columnDefinition = "int default 1")
    private Integer targetReadCount;

    @Column(name = "registration_start")
    private LocalDate registrationStart;

    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "pass_announcement_date")
    private LocalDate passAnnouncementDate;
}
