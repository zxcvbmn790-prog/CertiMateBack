package com.certimate.manager.community.service.impl;

import com.certimate.manager.community.entity.CommunityPostLike;
import com.certimate.manager.auth.entity.User;
import com.certimate.manager.community.dto.CommunityPostResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.community.repository.CommunityPostLikeRepository;
import com.certimate.manager.community.repository.CommunityPostRepository;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final UserRepository userRepository;

    @Override
    public List<CommunityPostResponse> getMyPosts(String email) {
        User user = getUserOrThrow(email);
        return communityPostRepository.findByNicknameOrderByCreatedAtDesc(user.getId()).stream()
                .map(CommunityPostResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityPostResponse> getLikedPosts(String email) {
        User user = getUserOrThrow(email);

        // 내가 좋아요 누른 이력 가져오기
        List<CommunityPostLike> likes = communityPostLikeRepository.findByNickname(user.getId());

        // 좋아요 누른 게시글 ID 추출
        List<Long> postIds = likes.stream()
                .map(CommunityPostLike::getPostId)
                .collect(Collectors.toList());

        // ID 리스트로 원본 게시글 가져오기
        return communityPostRepository.findAllById(postIds).stream()
                .map(CommunityPostResponse::from)
                .collect(Collectors.toList());
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."));
    }
}
