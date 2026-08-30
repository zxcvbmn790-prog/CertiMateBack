package com.certimate.manager.community.service;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.community.dto.CommentsRequestDto;
import com.certimate.manager.community.dto.CommentsResponseDto;
import com.certimate.manager.community.dto.CommunityPostRequestDto;
import com.certimate.manager.community.dto.CommunityPostResponseDto;
import com.certimate.manager.community.entity.Comments;
import com.certimate.manager.community.entity.CommunityPost;
import com.certimate.manager.community.repository.CommentsRepository;
import com.certimate.manager.community.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostService {

    private final CommunityPostRepository communityPostRepository;
    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository;

    // 첨부 이미지 파일 저장 경로 (프로젝트 실행 위치 하위 uploads/)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    /**
     * 커뮤니티 게시글 등록 (글쓰기)
     * nickname(int)으로 USER 테이블에서 user_id를 조회하여 작성자 name을 가져옵니다.
     * name이 없거나 유저를 찾지 못하면 'John Doe'가 기본값으로 적용됩니다.
     */
    @Transactional
    public CommunityPostResponseDto createPost(CommunityPostRequestDto requestDto, MultipartFile image) {
        String imageUrl = null;

        // 1. 첨부 이미지 파일 저장 처리
        if (image != null && !image.isEmpty()) {
            try {
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String originalFilename = image.getOriginalFilename();
                String storeFilename = UUID.randomUUID().toString() + "_" + (originalFilename != null ? originalFilename : "image.png");
                Path filePath = Paths.get(uploadDir + storeFilename);
                Files.copy(image.getInputStream(), filePath);

                imageUrl = "/uploads/" + storeFilename;
            } catch (IOException e) {
                throw new RuntimeException("이미지 파일 저장 중 오류가 발생했습니다.", e);
            }
        }

        // 2. nickname(int) 또는 userId 정보로 USER 테이블 조회
        Integer nicknameInt = requestDto.getNickname();
        if (nicknameInt == null && requestDto.getUserId() != null) {
            nicknameInt = requestDto.getUserId().intValue();
        }

        User user = null;
        if (nicknameInt != null) {
            user = userRepository.findById(nicknameInt.longValue()).orElse(null);
        }

        // 3. CommunityPost 엔티티 생성 및 DB 저장
        Long finalUserId = requestDto.getUserId() != null ? requestDto.getUserId() : (nicknameInt != null ? nicknameInt.longValue() : null);
        CommunityPost post = CommunityPost.builder()
                .category(requestDto.getCategory() != null && !requestDto.getCategory().isBlank() ? requestDto.getCategory() : "자유게시판")
                .nickname(nicknameInt)
                .userId(finalUserId)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .imageUrl(imageUrl)
                .views(0)
                .recommendations(0)
                .build();

        CommunityPost savedPost = communityPostRepository.save(post);
        return CommunityPostResponseDto.fromEntity(savedPost, user, Collections.emptyList());
    }

    /**
     * 유저 엔티티 바인딩 헬퍼 함수
     */
    private User resolveUser(CommunityPost post) {
        if (post.getUser() != null) {
            return post.getUser();
        }
        if (post.getUserId() != null) {
            return userRepository.findById(post.getUserId()).orElse(null);
        }
        if (post.getNickname() != null) {
            return userRepository.findById(post.getNickname().longValue()).orElse(null);
        }
        return null;
    }

    /**
     * 전체 게시글 목록 조회
     */
    public List<CommunityPostResponseDto> getAllPosts() {
        return communityPostRepository.findAllByOrderByPostIdDesc().stream()
                .map(post -> {
                    List<CommentsResponseDto> replyList = getCommentsByPostId(post.getPostId());
                    User user = resolveUser(post);
                    return CommunityPostResponseDto.fromEntity(post, user, replyList);
                })
                .collect(Collectors.toList());
    }

    /**
     * BEST 인기글 (조회수 기준 내림차순 상위 5개) 조회
     */
    public List<CommunityPostResponseDto> getBestPosts() {
        return communityPostRepository.findTop5ByOrderByViewsDesc().stream()
                .map(post -> {
                    List<CommentsResponseDto> replyList = getCommentsByPostId(post.getPostId());
                    User user = resolveUser(post);
                    return CommunityPostResponseDto.fromEntity(post, user, replyList);
                })
                .collect(Collectors.toList());
    }

    /**
     * 게시글 상세 조회 및 조회수 증가
     */
    @Transactional
    public CommunityPostResponseDto getPostDetail(Long id) {
        CommunityPost post = communityPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. id=" + id));
        post.setViews((post.getViews() != null ? post.getViews() : 0) + 1);

        List<CommentsResponseDto> replyList = getCommentsByPostId(id);
        User user = resolveUser(post);
        return CommunityPostResponseDto.fromEntity(post, user, replyList);
    }

    /**
     * 게시글 추천(좋아요) 수 1 증가
     */
    @Transactional
    public CommunityPostResponseDto recommendPost(Long id) {
        CommunityPost post = communityPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. id=" + id));
        post.setRecommendations((post.getRecommendations() != null ? post.getRecommendations() : 0) + 1);

        List<CommentsResponseDto> replyList = getCommentsByPostId(id);
        User user = resolveUser(post);
        return CommunityPostResponseDto.fromEntity(post, user, replyList);
    }

    /**
     * 댓글(comments) 및 대댓글 등록 API
     */
    @Transactional
    public CommentsResponseDto createComment(CommentsRequestDto requestDto) {
        CommunityPost post = communityPostRepository.findById(requestDto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. id=" + requestDto.getPostId()));

        User user = null;
        if (requestDto.getUserId() != null) {
            user = userRepository.findById(requestDto.getUserId()).orElse(null);
        }

        Comments parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentsRepository.findById(requestDto.getParentId()).orElse(null);
        }

        Comments comments = Comments.builder()
                .post(post)
                .parent(parent)
                .writer(requestDto.getWriter() != null && !requestDto.getWriter().isBlank() ? requestDto.getWriter() : "John Doe")
                .content(requestDto.getContent())
                .user(user)
                .build();

        Comments savedComments = commentsRepository.save(comments);
        return CommentsResponseDto.fromEntity(savedComments);
    }

    /**
     * 게시글 ID에 해당하는 댓글 목록 조회 (계층형 대댓글 포함)
     */
    public List<CommentsResponseDto> getCommentsByPostId(Long postId) {
        return commentsRepository.findByPostPostIdAndParentIsNullOrderByCommentIdAsc(postId).stream()
                .map(CommentsResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
