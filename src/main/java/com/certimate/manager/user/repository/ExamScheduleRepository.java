package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByCertification_IdAndExamDateAfterOrderByExamDateAsc(Long certId, LocalDate date);
    List<ExamSchedule> findByCertification_IdAndUserIsNullAndExamDateAfterOrderByExamDateAsc(Long certId, LocalDate date);
    List<ExamSchedule> findByQualNameAndUserIsNullAndExamDateAfterOrderByExamDateAsc(String qualName, LocalDate date);
    List<ExamSchedule> findByUser_IdAndCertification_IdAndExamDateAfterOrderByExamDateAsc(Long userId, Long certId, LocalDate date);
    List<ExamSchedule> findByUser_IdAndExamDateAfterOrderByExamDateAsc(Long userId, LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM ExamSchedule e JOIN FETCH e.user u LEFT JOIN FETCH e.certification c WHERE e.user IS NOT NULL AND e.examDate = :targetDate")
    List<ExamSchedule> findAllWithUserByExamDate(@org.springframework.data.repository.query.Param("targetDate") LocalDate targetDate);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM ExamSchedule e JOIN FETCH e.user u LEFT JOIN FETCH e.certification c WHERE e.user IS NOT NULL AND e.examDate >= :today")
    List<ExamSchedule> findAllActiveUserSchedules(@org.springframework.data.repository.query.Param("today") LocalDate today);
}

