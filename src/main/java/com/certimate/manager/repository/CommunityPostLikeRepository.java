package com.certimate.manager.repository;

import com.certimate.manager.domain.entity.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    List<CommunityPostLike> findByNickname(Long nickname);
}
