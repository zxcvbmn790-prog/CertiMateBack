package com.certimate.manager.user.dto;

import java.time.LocalDate;

public record AddScheduleRequest(Long certId, String examType, LocalDate examDate, Integer targetReadCount) {}
