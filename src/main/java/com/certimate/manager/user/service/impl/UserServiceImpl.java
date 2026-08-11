package com.certimate.manager.user.service.impl;

import com.certimate.manager.exam.entity.AiLearn;
import com.certimate.manager.user.entity.ExamSchedule;
import com.certimate.manager.auth.entity.User;
import com.certimate.manager.user.entity.UserLearnLog;
import com.certimate.manager.exam.entity.UserQuizHistory;
import com.certimate.manager.user.entity.UserScrap;
import com.certimate.manager.user.dto.DashboardResponse;
import com.certimate.manager.user.dto.QuizSessionResponse;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.exam.repository.AiLearnRepository;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.certimate.manager.user.repository.UserCertificationRepository;
import com.certimate.manager.user.repository.UserLearnLogRepository;
import com.certimate.manager.exam.repository.UserQuizHistoryRepository;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.user.repository.UserScrapRepository;
import com.certimate.manager.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final UserScrapRepository userScrapRepository;
    private final UserLearnLogRepository userLearnLogRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserQuizHistoryRepository userQuizHistoryRepository;
    private final AiLearnRepository aiLearnRepository;

    @Override
    public DashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        int certCount = userCertificationRepository.countByUser_Id(user.getId());

        List<UserScrap> scraps = userScrapRepository.findByUser_Id(user.getId());
        int scrapCount = scraps.size();
        List<DashboardResponse.ScrapItem> scrapItems = scraps.stream()
                .map(s -> new DashboardResponse.ScrapItem(s.getCertification().getCertName(), s.getId().toString()))
                .collect(Collectors.toList());

        List<UserLearnLog> logs = userLearnLogRepository.findByUser_Id(user.getId());
        int totalStudyMin = logs.stream().mapToInt(UserLearnLog::getStudyTimeMin).sum();
        double avgCorrectRate = logs.stream().mapToDouble(UserLearnLog::getCorrectRate).average().orElse(0.0);

        // 1. Heatmap Data (최근 1년 퀴즈 기록)
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<UserQuizHistory> quizHistory = userQuizHistoryRepository.findByUserIdAndSolvedAtAfter(user.getId(), oneYearAgo);

        Map<String, Long> dateCounts = quizHistory.stream()
                .filter(q -> q.getSolvedAt() != null)
                .collect(Collectors.groupingBy(
                        q -> q.getSolvedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        Collectors.counting()
                ));

        List<DashboardResponse.HeatmapPoint> heatmapData = dateCounts.entrySet().stream()
                .map(e -> new DashboardResponse.HeatmapPoint(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toList());

        // 2. Target Exam D-Day
        DashboardResponse.TargetExam targetExam = null;
        UserLearnLog recentLog = logs.stream()
                .max(Comparator.comparing(UserLearnLog::getLastStudiedAt))
                .orElse(null);

        if (recentLog != null) {
            List<ExamSchedule> schedules = examScheduleRepository.findByCertification_IdAndExamDateAfterOrderByExamDateAsc(
                    recentLog.getCertification().getId(), LocalDate.now().minusDays(1));

            if (!schedules.isEmpty()) {
                ExamSchedule upcoming = schedules.get(0);
                long dDay = ChronoUnit.DAYS.between(LocalDate.now(), upcoming.getExamDate());
                targetExam = new DashboardResponse.TargetExam(
                        recentLog.getCertification().getCertName(),
                        upcoming.getExamType(),
                        upcoming.getExamDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        dDay,
                        String.format("%.0f", recentLog.getCorrectRate())
                );
            }
        }

        return new DashboardResponse(
                certCount,
                scrapCount,
                scrapItems,
                totalStudyMin / 60 + "h",
                String.format("%.0f%%", avgCorrectRate),
                heatmapData,
                targetExam
        );
    }

    @Override
    public List<QuizSessionResponse> getQuizHistory(String email, String date) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateTime start = parsedDate.atStartOfDay();
        LocalDateTime end = parsedDate.atTime(23, 59, 59, 999999999);

        List<UserQuizHistory> history = userQuizHistoryRepository.findByUserIdAndSolvedAtBetween(user.getId(), start, end);
        history.sort(Comparator.comparing(UserQuizHistory::getSolvedAt));

        List<QuizSessionResponse> sessions = new ArrayList<>();
        List<QuizSessionResponse.QuizHistoryItemResponse> currentSessionRecords = new ArrayList<>();
        LocalDateTime prevTime = null;
        int sessionNum = 1;

        for (UserQuizHistory h : history) {
            if (prevTime != null && ChronoUnit.MINUTES.between(prevTime, h.getSolvedAt()) > 1) {
                if (!currentSessionRecords.isEmpty()) {
                    sessions.add(new QuizSessionResponse(sessionNum++, currentSessionRecords.get(0).solvedAtStr(), new ArrayList<>(currentSessionRecords)));
                    currentSessionRecords.clear();
                }
            }

            AiLearn learn = aiLearnRepository.findById(h.getLearnId()).orElse(null);
            if (learn != null) {
                currentSessionRecords.add(new QuizSessionResponse.QuizHistoryItemResponse(
                        learn.getLearnId(),
                        learn.getQuestion(),
                        learn.getOptions(),
                        h.getUserAnswer(),
                        learn.getAnswer(),
                        h.getIsCorrect(),
                        learn.getExplanation(),
                        h.getSolvedAt().format(DateTimeFormatter.ofPattern("a h:mm"))
                ));
            }
            prevTime = h.getSolvedAt();
        }
        if (!currentSessionRecords.isEmpty()) {
            sessions.add(new QuizSessionResponse(sessionNum, currentSessionRecords.get(0).solvedAtStr(), currentSessionRecords));
        }

        return sessions;
    }
}
