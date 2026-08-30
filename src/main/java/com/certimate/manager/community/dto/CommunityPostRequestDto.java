package com.certimate.manager.community.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostRequestDto {
    private Integer nickname;
    private String category;
    private String title;
    private String content;
    private Long userId;
}
