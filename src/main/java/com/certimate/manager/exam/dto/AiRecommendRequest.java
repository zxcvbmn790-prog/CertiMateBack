package com.certimate.manager.exam.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiRecommendRequest {
    private List<String> tags;
    private String customInput;
}
