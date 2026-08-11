package com.certimate.manager.dto.request;

public record UpdateProfileRequest(
        String name,
        String major,
        String interest,
        String status,
        String password,
        String profileImage,
        Boolean agreeConsent
) {
}
