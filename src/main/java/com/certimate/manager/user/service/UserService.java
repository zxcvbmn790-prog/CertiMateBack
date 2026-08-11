package com.certimate.manager.user.service;

import com.certimate.manager.user.dto.DashboardResponse;
import com.certimate.manager.user.dto.QuizSessionResponse;

import java.util.List;

public interface UserService {
    DashboardResponse getDashboard(String email);
    List<QuizSessionResponse> getQuizHistory(String email, String date);
}
