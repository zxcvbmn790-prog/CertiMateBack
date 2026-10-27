package com.certimate.manager.community.dto;

import com.certimate.manager.community.entity.Comments;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentsResponseDto {
    private Long id;
    private Long postId;
    private Long parentId;
    private String writer;
    private String content;
    private String date;

    @Builder.Default
    private List<CommentsResponseDto> children = new ArrayList<>();

    /** 답글을 건드리지 않는 변환. 트리는 호출하는 쪽이 만든다 (지연 로딩 N+1 방지). */
    public static CommentsResponseDto fromEntityWithoutChildren(Comments comment) {
        return CommentsResponseDto.builder()
                .id(comment.getCommentId())
                .postId(comment.getPost() != null ? comment.getPost().getPostId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getCommentId() : null)
                .writer(comment.getWriter())
                .content(comment.getContent())
                .date(comment.getCreatedAt() != null ? comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) : "")
                .build();
    }

    public static CommentsResponseDto fromEntity(Comments comment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        List<CommentsResponseDto> childDtos = (comment.getChildren() != null)
                ? comment.getChildren().stream().map(CommentsResponseDto::fromEntity).collect(Collectors.toList())
                : new ArrayList<>();

        return CommentsResponseDto.builder()
                .id(comment.getCommentId())
                .postId(comment.getPost() != null ? comment.getPost().getPostId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getCommentId() : null)
                .writer(comment.getWriter())
                .content(comment.getContent())
                .date(comment.getCreatedAt() != null ? comment.getCreatedAt().format(formatter) : "")
                .children(childDtos)
                .build();
    }
}
