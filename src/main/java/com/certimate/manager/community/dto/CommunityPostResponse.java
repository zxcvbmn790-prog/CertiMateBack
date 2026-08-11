package com.certimate.manager.community.dto;

import com.certimate.manager.community.entity.CommunityPost;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long id,
        String category,
        String title,
        String content,
        String imageUrl,
        Integer views,
        Integer recommendations,
        LocalDateTime createdAt
) {
    public static CommunityPostResponse from(CommunityPost post) {
        return new CommunityPostResponse(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                post.getViews(),
                post.getRecommendations(),
                post.getCreatedAt()
        );
    }
}
