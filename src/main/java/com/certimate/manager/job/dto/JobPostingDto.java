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
public class JobPostingDto {
    private String id;
    private String title;
    private String companyName;
    private String location;
    private String experience;
    private String education;
    private String employmentType;
    private String salary;
    private String deadline;
    private List<String> sectors;
    private String badge;
    private String url;
    private String matchedCert;
    private boolean isMyCert;
    private int matchScore;
}
