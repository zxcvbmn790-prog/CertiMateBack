package com.certimate.manager.exam.repository;

import com.certimate.manager.exam.entity.AiLearn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiLearnRepository extends JpaRepository<AiLearn, Long> {
    List<AiLearn> findByCertIdAndSubjectNum(Long certId, Integer subjectNum);

    // MySQL의 RAND() 함수를 사용하여 랜덤 출제
    @Query(value = "SELECT * FROM AI_LEARN WHERE cert_id = :certId AND subject_num = :subjectNum ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<AiLearn> findRandomQuestionsBySubject(
            @Param("certId") Long certId,
            @Param("subjectNum") Integer subjectNum,
            @Param("limit") int limit
    );

    // 한 문제씩 풀기(무한 학습) 모드: 과목 구분 없이 랜덤 1문제 출제
    @Query(value = "SELECT * FROM AI_LEARN WHERE cert_id = :certId ORDER BY RAND() LIMIT 1", nativeQuery = true)
    List<AiLearn> findRandomQuestion(@Param("certId") Long certId);

    // 직전에 풀었던 문제들은 제외하고 랜덤 1문제 출제 (연속 중복 방지)
    @Query(value = "SELECT * FROM AI_LEARN WHERE cert_id = :certId AND learn_id NOT IN (:excludeIds) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    List<AiLearn> findRandomQuestionExcluding(@Param("certId") Long certId, @Param("excludeIds") List<Long> excludeIds);
}
