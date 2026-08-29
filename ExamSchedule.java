package com.certimate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "qual_name", nullable = false, length = 100)
    private String qualName;

    @Column(name = "exam_round", length = 100)
    private String examRound;

    @Column(name = "exam_date", length = 100)
    private String examDate;
}
