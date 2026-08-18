package com.certimate;

import com.certimate.dto.CommentsRequestDto;
import com.certimate.dto.CommentsResponseDto;
import com.certimate.dto.CommunityPostRequestDto;
import com.certimate.dto.CommunityPostResponseDto;
import com.certimate.service.CommunityPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * CertiMate 커뮤니티 (Community) REST Controller
 * Community.jsx 의 글쓰기(별명, 분류, 제목, 내용, 사진첨부) 및 댓글(comments) 요청을 처리합니다.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://3.39.237.132:3306")
public class comty {

    private final CommunityPostService communityPostService;

    /**
     * [등록하기 버튼] 새 게시글 작성 API
     * nickname(int, USER.user_id 참조)을 전달 받아 USER 테이블의 name을 조회합니다.
     */
    @PostMapping(value = "/write", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("content") String content,
            @RequestParam(value = "nickname", required = false) Integer nickname,
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
     * 클릭 시 DB의 recommendations 숫자가 1 증가합니다.
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
}
