package com.certimate.manager.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "exam_schedule")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certification certification;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "registration_start")
    private LocalDate registrationStart;

    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "pass_announcement_date")
    private LocalDate passAnnouncementDate;
}
