package com.certimate.manager.community.service;

import com.certimate.manager.community.dto.CommunityPostResponse;

import java.util.List;

public interface CommunityService {
    List<CommunityPostResponse> getMyPosts(String email);
    List<CommunityPostResponse> getLikedPosts(String email);
}
