package com.certimate.manager.community.repository;

import com.certimate.manager.community.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findByNicknameOrderByCreatedAtDesc(Long nickname);
}
