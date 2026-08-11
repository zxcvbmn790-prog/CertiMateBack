package com.certimate.manager.dto.response;

import com.certimate.manager.domain.entity.Example;
import java.time.LocalDateTime;

public record ExampleResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {
    public static ExampleResponse from(Example example) {
        return new ExampleResponse(example.getId(), example.getName(), example.getCreatedAt());
    }
}
