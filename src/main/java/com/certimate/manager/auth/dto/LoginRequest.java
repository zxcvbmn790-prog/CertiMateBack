package com.certimate.manager.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}
