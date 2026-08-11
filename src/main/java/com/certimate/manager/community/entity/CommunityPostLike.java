package com.certimate.manager.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_post_like")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommunityPostLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    // 누가 좋아요를 눌렀는지 식별하는 용도 (User 테이블의 ID)
    @Column(name = "nickname", nullable = false)
    private Long nickname;
}
