package com.certimate.manager.community.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostUpdateRequestDto {
    private String title;
    private String category;
    private String content;
    private Boolean removeImage;
}
