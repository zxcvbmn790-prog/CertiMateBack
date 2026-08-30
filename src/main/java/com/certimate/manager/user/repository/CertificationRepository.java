package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    Optional<Certification> findByCertName(String certName);

    // 실제 문제가 등록된 자격증만 과목별 문항수와 함께 조회 (문제 0개인 자격증은 목록에서 제외)
    @Query(value = """
            SELECT c.cert_id AS certId, c.cert_name AS certName, c.difficulty AS difficulty, c.agency AS agency,
                   COUNT(a.learn_id) AS totalQuestions,
                   CAST(SUM(a.subject_num = 1) AS SIGNED) AS subject1,
                   CAST(SUM(a.subject_num = 2) AS SIGNED) AS subject2,
                   CAST(SUM(a.subject_num = 3) AS SIGNED) AS subject3
            FROM certification c
            JOIN ai_learn a ON a.cert_id = c.cert_id
            GROUP BY c.cert_id, c.cert_name, c.difficulty, c.agency
            ORDER BY totalQuestions DESC
            """, nativeQuery = true)
    List<CertSummaryProjection> findCertificationsWithQuestions();

    interface CertSummaryProjection {
        Long getCertId();
        String getCertName();
        String getDifficulty();
        String getAgency();
        Long getTotalQuestions();
        Long getSubject1();
        Long getSubject2();
        Long getSubject3();
    }
}
