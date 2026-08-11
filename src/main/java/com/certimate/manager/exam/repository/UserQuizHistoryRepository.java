package com.certimate.manager.exam.repository;

import com.certimate.manager.exam.entity.UserQuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserQuizHistoryRepository extends JpaRepository<UserQuizHistory, Long> {
    List<UserQuizHistory> findByUserIdAndSolvedAtAfter(Long userId, LocalDateTime startDate);
    List<UserQuizHistory> findByUserIdAndSolvedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    void deleteByUserId(Long userId);
}
