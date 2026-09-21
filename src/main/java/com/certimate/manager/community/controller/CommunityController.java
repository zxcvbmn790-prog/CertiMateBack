package com.certimate.manager.community.controller;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
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
 * 커뮤니티 글쓰기, 목록/상세 조회, 추천, 수정, 삭제, 댓글, 마이페이지 작성글 조회를 처리합니다.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityPostService communityPostService;
    private final UserRepository userRepository;

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
            @RequestParam(value = "image", required = false) MultipartFile image,
            Principal principal
    ) {
        try {
            Long finalUserId = userId;
            Integer finalNickname = nickname;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    finalUserId = user.getId();
                    finalNickname = user.getId().intValue();
                }
            }

            CommunityPostRequestDto requestDto = CommunityPostRequestDto.builder()
                    .title(title)
                    .category(category)
                    .content(content)
                    .nickname(finalNickname)
                    .userId(finalUserId)
                    .build();

            CommunityPostResponseDto responseDto = communityPostService.createPost(requestDto, image);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("게시글 등록에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 게시글 수정 API
     * 작성자 본인만 수정 가능합니다.
     */
    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePost(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("content") String content,
            @RequestParam(value = "removeImage", required = false, defaultValue = "false") Boolean removeImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            Principal principal
    ) {
        try {
            Long currentUserId = null;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getId();
                }
            }
            if (currentUserId == null && requestUserId != null) {
                currentUserId = requestUserId;
            }
            if (currentUserId == null) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            }

            CommunityPostUpdateRequestDto requestDto = CommunityPostUpdateRequestDto.builder()
                    .title(title)
                    .category(category)
                    .content(content)
                    .removeImage(removeImage)
                    .build();

            CommunityPostResponseDto responseDto = communityPostService.updatePost(id, currentUserId, requestDto, image);
            return ResponseEntity.ok(responseDto);
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("게시글 수정에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 게시글 삭제 API
     * 작성자 본인만 삭제 가능합니다.
     * 연관된 댓글/대댓글, 좋아요, 첨부 이미지, 게시글 데이터가 모두 함께 삭제됩니다.
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(
            @PathVariable("id") Long id,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            Principal principal
    ) {
        try {
            Long currentUserId = null;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getId();
                }
            }
            if (currentUserId == null && requestUserId != null) {
                currentUserId = requestUserId;
            }
            if (currentUserId == null) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            }

            communityPostService.deletePost(id, currentUserId);
            return ResponseEntity.ok("게시글이 성공적으로 삭제되었습니다.");
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("게시글 삭제에 실패했습니다: " + e.getMessage());
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
     * BEST 인기글 (조회순 또는 추천순 기준 내림차순 상위 5개) 조회 API
     * sort: "views" (기본값, 조회순), "recommendations" (추천순)
     */
    @GetMapping("/posts/best")
    public ResponseEntity<List<CommunityPostResponseDto>> getBestPosts(
            @RequestParam(value = "sort", required = false, defaultValue = "views") String sort
    ) {
        List<CommunityPostResponseDto> bestPosts = communityPostService.getBestPosts(sort);
        return ResponseEntity.ok(bestPosts);
    }

    /**
     * 게시글 상세 조회 API
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<CommunityPostResponseDto> getPostDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            Principal principal
    ) {
        Long currentUserId = null;
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                currentUserId = user.getId();
            }
        }
        if (currentUserId == null && requestUserId != null) {
            currentUserId = requestUserId;
        }

        CommunityPostResponseDto post = communityPostService.getPostDetail(id, currentUserId);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 추천 (좋아요) API
     */
    @PostMapping("/posts/{id}/recommend")
    public ResponseEntity<?> recommendPost(
            @PathVariable("id") Long id,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            @RequestParam(value = "liked", required = false) Boolean liked,
            Principal principal
    ) {
        try {
            Long currentUserId = null;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getId();
                }
            }
            if (currentUserId == null && requestUserId != null) {
                currentUserId = requestUserId;
            }

            CommunityPostResponseDto responseDto = communityPostService.toggleRecommendPost(id, currentUserId, liked);
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
