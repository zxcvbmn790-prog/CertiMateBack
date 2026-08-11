package com.certimate.manager.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String name,
        String major,
        String interest,
        String status,
        Boolean agreeConsent
) {
}
