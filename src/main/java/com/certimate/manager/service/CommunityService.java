package com.certimate.manager.service;

import com.certimate.manager.dto.response.CommunityPostResponse;

import java.util.List;

public interface CommunityService {
    List<CommunityPostResponse> getMyPosts(String email);
    List<CommunityPostResponse> getLikedPosts(String email);
}
