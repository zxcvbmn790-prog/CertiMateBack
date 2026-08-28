package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByCertification_IdAndExamDateAfterOrderByExamDateAsc(Long certId, LocalDate date);
    List<ExamSchedule> findByUser_IdAndCertification_IdAndExamDateAfterOrderByExamDateAsc(Long userId, Long certId, LocalDate date);
    List<ExamSchedule> findByUser_IdAndExamDateAfterOrderByExamDateAsc(Long userId, LocalDate date);
}

