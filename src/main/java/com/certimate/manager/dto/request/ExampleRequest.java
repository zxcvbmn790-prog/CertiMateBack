package com.certimate.manager.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExampleRequest(
        @NotBlank(message = "name은 필수입니다") String name
) {
}
