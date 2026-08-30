package com.certimate.manager.community.repository;

import com.certimate.manager.community.entity.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {
    List<Comments> findByPostPostIdOrderByCommentIdAsc(Long postId);
    List<Comments> findByPostPostIdAndParentIsNullOrderByCommentIdAsc(Long postId);
    long countByPostPostId(Long postId);
}
