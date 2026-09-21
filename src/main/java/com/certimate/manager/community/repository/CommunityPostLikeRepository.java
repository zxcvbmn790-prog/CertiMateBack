package com.certimate.manager.community.repository;

import com.certimate.manager.community.entity.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    List<CommunityPostLike> findByNickname(Long nickname);

    List<CommunityPostLike> findByPostIdAndNickname(Long postId, Long nickname);

    @Modifying
    @Query("DELETE FROM CommunityPostLike c WHERE c.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
