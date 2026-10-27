package com.certimate.manager.community;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.community.controller.CommunityController;
import com.certimate.manager.community.dto.CommentsRequestDto;
import com.certimate.manager.community.dto.CommentsResponseDto;
import com.certimate.manager.community.service.CommunityPostService;
import com.certimate.manager.community.service.CommunityService;
import com.certimate.manager.config.SecurityConfig;
import com.certimate.manager.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /api/community/** 는 permitAll이다. 쓰기 API는 컨트롤러에서 로그인을 확인하고,
// 요청의 userId/writer가 아니라 로그인 정보로 사용자를 정해야 한다 (남의 글 삭제·수정 위조 방지).
@WebMvcTest(CommunityController.class)
@Import(SecurityConfig.class)
class CommunityAuthTest {

    @Autowired MockMvc mvc;
    @MockBean CommunityPostService communityPostService;
    @MockBean CommunityService communityService;
    @MockBean UserRepository userRepository;
    @MockBean JwtProvider jwtProvider;

    @Test
    void anonymousWritesAreRejectedEvenWithUserIdParam() throws Exception {
        mvc.perform(delete("/api/community/posts/5").param("userId", "1")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/community/posts/5").with(r -> { r.setMethod("PUT"); return r; })
                        .param("title", "t").param("category", "자유게시판").param("content", "c").param("userId", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/community/write")
                        .param("title", "t").param("category", "자유게시판").param("content", "c").param("userId", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/community/posts/5/recommend").param("userId", "1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/community/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":5,\"content\":\"x\",\"userId\":1,\"writer\":\"관리자\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(communityPostService);
    }

    @Test
    void loggedInDeleteUsesOwnIdNotParam() throws Exception {
        loginAs(7L, "나");

        mvc.perform(delete("/api/community/posts/5").param("userId", "1").cookie(new Cookie("accessToken", "ok")))
                .andExpect(status().isOk());

        verify(communityPostService).deletePost(5L, 7L);
    }

    @Test
    void loggedInCommentAuthorComesFromLogin() throws Exception {
        loginAs(7L, "나");
        when(communityPostService.createComment(any())).thenReturn(new CommentsResponseDto());

        mvc.perform(post("/api/community/comments").cookie(new Cookie("accessToken", "ok"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":5,\"content\":\"x\",\"userId\":1,\"writer\":\"관리자\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CommentsRequestDto> captor = ArgumentCaptor.forClass(CommentsRequestDto.class);
        verify(communityPostService).createComment(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getWriter()).isEqualTo("나");
    }

    private void loginAs(Long id, String name) {
        when(jwtProvider.validateToken("ok")).thenReturn(true);
        when(jwtProvider.getEmailFromToken("ok")).thenReturn("u@test.com");
        User me = mock(User.class);
        when(me.getId()).thenReturn(id);
        when(me.getName()).thenReturn(name);
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(userRepository.findById(id)).thenReturn(Optional.of(me));
    }
}
