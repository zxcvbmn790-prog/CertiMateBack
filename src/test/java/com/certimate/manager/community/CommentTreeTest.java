package com.certimate.manager.community;

import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.community.dto.CommentsResponseDto;
import com.certimate.manager.community.entity.Comments;
import com.certimate.manager.community.entity.CommunityPost;
import com.certimate.manager.community.repository.CommentsRepository;
import com.certimate.manager.community.repository.CommunityPostLikeRepository;
import com.certimate.manager.community.repository.CommunityPostRepository;
import com.certimate.manager.community.service.CommunityPostService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// 댓글 답글 트리를 쿼리 한 번으로 만든다 (댓글마다 답글을 지연 로딩하던 N+1 제거).
class CommentTreeTest {

    private final CommentsRepository commentsRepository = mock(CommentsRepository.class);
    private final CommunityPostService service = new CommunityPostService(
            mock(CommunityPostRepository.class), commentsRepository, mock(CommunityPostLikeRepository.class), mock(UserRepository.class));

    @Test
    void buildsTreeFromOneQuery() {
        CommunityPost post = CommunityPost.builder().postId(1L).title("t").content("c").build();
        Comments root = Comments.builder().commentId(10L).post(post).writer("a").content("부모").build();
        Comments reply = Comments.builder().commentId(11L).post(post).parent(root).writer("b").content("답글").build();
        Comments other = Comments.builder().commentId(12L).post(post).writer("c").content("다른 댓글").build();
        when(commentsRepository.findByPostPostIdOrderByCommentIdAsc(1L)).thenReturn(List.of(root, reply, other));

        List<CommentsResponseDto> tree = service.getCommentsByPostId(1L);

        assertThat(tree).extracting(CommentsResponseDto::getId).containsExactly(10L, 12L);
        assertThat(tree.get(0).getChildren()).extracting(CommentsResponseDto::getId).containsExactly(11L);
        verify(commentsRepository, times(1)).findByPostPostIdOrderByCommentIdAsc(1L);
        verifyNoMoreInteractions(commentsRepository);
    }
}
