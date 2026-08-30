package com.certimate.manager.community.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.community.dto.*;
import com.certimate.manager.community.service.CommunityPostService;
import com.certimate.manager.community.service.CommunityService;
import com.certimate.manager.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * CertiMate 커뮤니티 (Community) REST Controller
 * 커뮤니티 글쓰기, 목록/상세 조회, 추천, 댓글, 마이페이지 작성글 조회를 처리합니다.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityPostService communityPostService;

    /**
     * [등록하기 버튼] 새 게시글 작성 API
     */
    @PostMapping(value = "/write", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("content") String content,
            @RequestParam(value = "user_id", required = false) Integer nickname,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            CommunityPostRequestDto requestDto = CommunityPostRequestDto.builder()
                    .title(title)
                    .category(category)
                    .content(content)
                    .nickname(nickname)
                    .userId(userId)
                    .build();

            CommunityPostResponseDto responseDto = communityPostService.createPost(requestDto, image);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("게시글 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 커뮤니티 전체 게시글 목록 조회 API
     */
    @GetMapping("/posts")
    public ResponseEntity<List<CommunityPostResponseDto>> getAllPosts() {
        List<CommunityPostResponseDto> posts = communityPostService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    /**
     * BEST 인기글 (조회수 기준 내림차순 상위 5개) 조회 API
     */
    @GetMapping("/posts/best")
    public ResponseEntity<List<CommunityPostResponseDto>> getBestPosts() {
        List<CommunityPostResponseDto> bestPosts = communityPostService.getBestPosts();
        return ResponseEntity.ok(bestPosts);
    }

    /**
     * 게시글 상세 조회 API
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<CommunityPostResponseDto> getPostDetail(@PathVariable("id") Long id) {
        CommunityPostResponseDto post = communityPostService.getPostDetail(id);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 추천 (좋아요) API
     */
    @PostMapping("/posts/{id}/recommend")
    public ResponseEntity<?> recommendPost(@PathVariable("id") Long id) {
        try {
            CommunityPostResponseDto responseDto = communityPostService.recommendPost(id);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("게시글 추천 처리에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 댓글(comments) 작성 API
     */
    @PostMapping("/comments")
    public ResponseEntity<?> createComment(@RequestBody CommentsRequestDto requestDto) {
        try {
            CommentsResponseDto responseDto = communityPostService.createComment(requestDto);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("댓글 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 게시글의 댓글(comments) 목록 조회 API
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentsResponseDto>> getComments(@PathVariable("postId") Long postId) {
        List<CommentsResponseDto> commentsList = communityPostService.getCommentsByPostId(postId);
        return ResponseEntity.ok(commentsList);
    }

    /**
     * 마이페이지 - 내가 쓴 글 목록 조회
     */
    @GetMapping("/my-posts")
    public ApiResponse<List<CommunityPostResponse>> getMyPosts(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(communityService.getMyPosts(principal.getName()));
    }

    /**
     * 마이페이지 - 내가 좋아요 누른 글 목록 조회
     */
    @GetMapping("/liked-posts")
    public ApiResponse<List<CommunityPostResponse>> getLikedPosts(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(communityService.getLikedPosts(principal.getName()));
    }

    private void requireAuth(Principal principal) {
        if (principal == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
    }
}
