package com.certimate.manager.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchingResponse {
    private String userName;
    private String userMajor;
    private String userInterest;
    private List<String> myCertifications;
    private List<String> acquiredCertifications;
    private List<String> targetCertifications;
    private String activeKeyword;
    private List<JobPostingDto> postings;
}
