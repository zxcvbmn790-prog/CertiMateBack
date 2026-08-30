package com.certimate.manager.community.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentsRequestDto {
    private Long postId;
    private Long parentId;
    private String writer;
    private String content;
    private Long userId;
}