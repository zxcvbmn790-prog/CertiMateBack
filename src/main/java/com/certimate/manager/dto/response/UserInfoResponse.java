package com.certimate.manager.dto.response;

import com.certimate.manager.domain.entity.User;

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
