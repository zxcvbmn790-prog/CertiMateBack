package com.certimate.manager.exam.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiCoverLetterRequest {
    private String companyName;
    private String jobRole;
    private List<String> tags;
    private List<String> experiences;
    private String pros;
    private String cons;
    private String additionalInfo;
    private boolean isFinal;
}
