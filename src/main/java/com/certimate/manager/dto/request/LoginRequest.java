package com.certimate.manager.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
