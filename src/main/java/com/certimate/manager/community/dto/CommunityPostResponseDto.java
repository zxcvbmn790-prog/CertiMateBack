package com.certimate.manager.community.dto;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.community.entity.CommunityPost;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostResponseDto {
    private Long id;
    private String category;
    private String title;
    private String content;
    private String writer;
    private Integer nickname;
    private Long userId;
    private String date;
    private Integer views;
    private Integer recommendations;
    private Integer comments;
    private String imageUrl;
    private List<CommentsResponseDto> replyList;

    @JsonProperty("isLiked")
    @Builder.Default
    private Boolean isLiked = false;

    public static CommunityPostResponseDto fromEntity(CommunityPost post, User user, List<CommentsResponseDto> replyList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        String writerName = "John Doe";
        User resolvedUser = (user != null) ? user : post.getUser();
        if (resolvedUser != null && resolvedUser.getName() != null && !resolvedUser.getName().isBlank()) {
            writerName = resolvedUser.getName();
        }

        Long finalUserId = post.getUserId();
        if (finalUserId == null && post.getNickname() != null) {
            finalUserId = post.getNickname().longValue();
        }
        if (finalUserId == null && resolvedUser != null) {
            finalUserId = resolvedUser.getId();
        }

        return CommunityPostResponseDto.builder()
                .id(post.getPostId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(writerName)
                .nickname(post.getNickname())
                .userId(finalUserId)
                .date(post.getCreatedAt() != null ? post.getCreatedAt().format(formatter) : "")
                .views(post.getViews())
                .recommendations(post.getRecommendations())
                .comments(replyList != null ? replyList.size() : 0)
                .imageUrl(post.getImageUrl())
                .replyList(replyList != null ? replyList : Collections.emptyList())
                .build();
    }

    public static CommunityPostResponseDto fromEntity(CommunityPost post, List<CommentsResponseDto> replyList) {
        return fromEntity(post, null, replyList);
    }

    public static CommunityPostResponseDto fromEntity(CommunityPost post) {
        return fromEntity(post, null, Collections.emptyList());
    }
}
