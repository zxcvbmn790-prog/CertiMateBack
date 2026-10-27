package com.certimate.manager.community.repository;

import com.certimate.manager.community.entity.CommunityPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    // 목록에서 작성자를 글마다 따로 조회하지 않도록 user를 함께 가져온다 (N+1 방지).
    @EntityGraph(attributePaths = "user")
    List<CommunityPost> findAllByOrderByPostIdDesc();
    @EntityGraph(attributePaths = "user")
    List<CommunityPost> findTop5ByOrderByViewsDesc();
    @EntityGraph(attributePaths = "user")
    List<CommunityPost> findTop5ByOrderByRecommendationsDesc();
    List<CommunityPost> findByNicknameOrderByCreatedAtDesc(Integer nickname);
}
