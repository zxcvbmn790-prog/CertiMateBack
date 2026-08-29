package com.certimate.repository;

import com.certimate.domain.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Integer> {

    /**
     * 자격증 명(qual_name)으로 시험 일정 목록 일치 조회
     */
    List<ExamSchedule> findByQualName(String qualName);

    /**
     * 자격증 명(qual_name)에 특정 키워드가 포함된 시험 일정 목록 조회
     */
    List<ExamSchedule> findByQualNameContaining(String qualName);

    /**
     * 시험 회차(exam_round)에 특정 키워드가 포함된 시험 일정 목록 조회
     */
    List<ExamSchedule> findByExamRoundContaining(String examRound);

    /**
     * 시험 날짜(exam_date)에 특정 키워드가 포함된 시험 일정 목록 조회
     */
    List<ExamSchedule> findByExamDateContaining(String examDate);

    /**
     * 자격증명, 회차, 날짜 통합 키워드 검색
     */
    @Query("SELECT e FROM ExamSchedule e WHERE " +
           "LOWER(e.qualName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.examRound) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.examDate) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ExamSchedule> searchByKeyword(@Param("keyword") String keyword);
}
