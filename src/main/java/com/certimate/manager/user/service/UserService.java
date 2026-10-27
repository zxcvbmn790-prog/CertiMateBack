package com.certimate.manager.user.service;

import com.certimate.manager.user.dto.AddAcquiredCertRequest;
import com.certimate.manager.user.dto.DashboardResponse;
import com.certimate.manager.user.dto.QuizSessionResponse;
import com.certimate.manager.user.dto.AddScheduleRequest;

import java.util.List;

public interface UserService {
    DashboardResponse getDashboard(String email);
    List<QuizSessionResponse> getQuizHistory(String email, String date);
    void addSchedule(String email, AddScheduleRequest request);
    void deleteSchedule(String email, Long id);
    void addAcquiredCertification(String email, AddAcquiredCertRequest request);
    void deleteAcquiredCertification(String email, Long id);
}
