package com.certimate.manager.exam.repository;

import com.certimate.manager.exam.entity.ExamLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamLocationRepository extends JpaRepository<ExamLocation, Long> {

    // 이름/주소 부분검색 (최대 50건)
    @Query(value = """
            SELECT * FROM qnet_exam_info
            WHERE test_site LIKE CONCAT('%', :q, '%') OR address LIKE CONCAT('%', :q, '%')
            LIMIT 50
            """, nativeQuery = true)
    List<ExamLocation> search(@Param("q") String q);

    // 내 주변: Haversine 거리(km) 계산 후 가까운 순 정렬. 좌표 없는 행은 제외.
    @Query(value = """
            SELECT id, qual_name AS qualName, exam_round AS examRound, exam_date AS examDate, test_site AS testSite, address, latitude, longitude,
                   (6371 * ACOS(
                       LEAST(1.0,
                         COS(RADIANS(:lat)) * COS(RADIANS(CAST(latitude AS DECIMAL(12,8)))) *
                         COS(RADIANS(CAST(longitude AS DECIMAL(12,8))) - RADIANS(:lng)) +
                         SIN(RADIANS(:lat)) * SIN(RADIANS(CAST(latitude AS DECIMAL(12,8))))
                       )
                   )) AS distanceKm
            FROM qnet_exam_info
            WHERE latitude IS NOT NULL AND latitude <> '' AND longitude IS NOT NULL AND longitude <> ''
            ORDER BY distanceKm ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<NearProjection> findNearest(@Param("lat") double lat, @Param("lng") double lng, @Param("limit") int limit);

    interface NearProjection {
        Long getId();
        String getQualName();
        String getExamRound();
        String getExamDate();
        String getTestSite();
        String getAddress();
        String getLatitude();
        String getLongitude();
        Double getDistanceKm();
    }
}
