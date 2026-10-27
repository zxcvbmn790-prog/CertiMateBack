package com.certimate.manager.community.repository;

import com.certimate.manager.community.entity.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {
    List<Comments> findByPostPostIdOrderByCommentIdAsc(Long postId);
    List<Comments> findByPostPostIdAndParentIsNullOrderByCommentIdAsc(Long postId);
    long countByPostPostId(Long postId);

    // 목록 화면은 댓글 개수만 쓴다. 글마다 세지 않고 한 번에 센다 (N+1 방지). [postId, count]
    @Query("select c.post.postId, count(c) from Comments c where c.post.postId in :postIds group by c.post.postId")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}
