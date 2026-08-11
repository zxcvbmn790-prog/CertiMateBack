package com.certimate.manager.community.controller;

import com.certimate.manager.common.ApiResponse;
import com.certimate.manager.community.dto.CommunityPostResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/my-posts")
    public ApiResponse<List<CommunityPostResponse>> getMyPosts(Principal principal) {
        requireAuth(principal);
        return ApiResponse.success(communityService.getMyPosts(principal.getName()));
    }

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
