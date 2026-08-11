package com.certimate.manager.auth.dto;

import com.certimate.manager.auth.entity.User;

public record UserInfoResponse(
        String name,
        String major,
        String interest,
        String status,
        String profileImage,
        Boolean agreeConsent
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getName(),
                user.getMajor(),
                user.getInterest(),
                user.getStatus(),
                user.getProfileImage(),
                user.getAgreeConsent()
        );
    }
}
