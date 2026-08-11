package com.certimate.manager.service;

import com.certimate.manager.dto.response.DashboardResponse;
import com.certimate.manager.dto.response.QuizSessionResponse;

import java.util.List;

public interface UserService {
    DashboardResponse getDashboard(String email);
    List<QuizSessionResponse> getQuizHistory(String email, String date);
}
