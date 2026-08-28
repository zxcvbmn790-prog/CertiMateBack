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
import com.certimate.manager.user.repository.CertificationRepository;
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
import java.util.Set;
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
    private final CertificationRepository certificationRepository;

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
        // To count sessions per cert later
        Set<Long> learnIds = quizHistory.stream().map(UserQuizHistory::getLearnId).collect(Collectors.toSet());
        List<com.certimate.manager.exam.entity.AiLearn> aiLearns = aiLearnRepository.findAllById(learnIds);
        Map<Long, Long> learnIdToCertId = aiLearns.stream().collect(Collectors.toMap(com.certimate.manager.exam.entity.AiLearn::getLearnId, com.certimate.manager.exam.entity.AiLearn::getCertId));

        DashboardResponse.TargetExam targetExam = null;
        if (!logs.isEmpty()) {
            logs.sort((a, b) -> {
                if (a.getLastStudiedAt() == null && b.getLastStudiedAt() == null) return 0;
                if (a.getLastStudiedAt() == null) return 1;
                if (b.getLastStudiedAt() == null) return -1;
                return b.getLastStudiedAt().compareTo(a.getLastStudiedAt());
            });
            com.certimate.manager.user.entity.UserLearnLog recentLog = logs.get(0);
            List<com.certimate.manager.user.entity.ExamSchedule> schedules = examScheduleRepository.findByUser_IdAndCertification_IdAndExamDateAfterOrderByExamDateAsc(
                    user.getId(), recentLog.getCertification().getId(), LocalDate.now().minusDays(1));

            if (!schedules.isEmpty()) {
                ExamSchedule upcoming = schedules.get(0);
                long dDay = ChronoUnit.DAYS.between(LocalDate.now(), upcoming.getExamDate());
                
                long solvedSessions = quizHistory.stream()
                        .filter(q -> learnIdToCertId.get(q.getLearnId()) != null && learnIdToCertId.get(q.getLearnId()).equals(recentLog.getCertification().getId()) && q.getSolvedAt() != null)
                        .map(q -> q.getSolvedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .distinct()
                        .count();
                int target = upcoming.getTargetReadCount() != null && upcoming.getTargetReadCount() > 0 ? upcoming.getTargetReadCount() : 1;
                float achievement = Math.min(100.0f, ((float) solvedSessions / target) * 100);

                targetExam = new DashboardResponse.TargetExam(
                        recentLog.getCertification().getCertName(),
                        upcoming.getExamType(),
                        upcoming.getExamDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        dDay,
                        String.format("%.0f", achievement)
                );
            }
        }

        String studyTimeStr = totalStudyMin < 60 ? totalStudyMin + "m" : (totalStudyMin / 60) + "h " + (totalStudyMin % 60) + "m";

        List<DashboardResponse.CertStat> certStats = logs.stream()
                .map(log -> {
                    int min = log.getStudyTimeMin() == null ? 0 : log.getStudyTimeMin();
                    String tStr = min < 60 ? min + "m" : (min / 60) + "h " + (min % 60) + "m";
                    float rate = log.getCorrectRate() == null ? 0.0f : log.getCorrectRate();
                    String aStr = String.format("%.0f%%", rate);
                    return new DashboardResponse.CertStat(log.getCertification().getId(), log.getCertification().getCertName(), tStr, aStr);
                })
                .collect(Collectors.toList());

        // Trend Data
        List<com.certimate.manager.user.entity.Certification> certifications = certificationRepository.findAll();
        Map<Long, String> certIdToName = certifications.stream().collect(Collectors.toMap(com.certimate.manager.user.entity.Certification::getId, com.certimate.manager.user.entity.Certification::getCertName));

        Map<Long, Map<String, List<UserQuizHistory>>> historyByCertAndDate = quizHistory.stream()
                .filter(q -> q.getSolvedAt() != null && learnIdToCertId.containsKey(q.getLearnId()))
                .collect(Collectors.groupingBy(
                        q -> learnIdToCertId.get(q.getLearnId()),
                        Collectors.groupingBy(q -> q.getSolvedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                ));

        List<DashboardResponse.CertTrend> certTrends = new ArrayList<>();
        for (Map.Entry<Long, Map<String, List<UserQuizHistory>>> entry : historyByCertAndDate.entrySet()) {
            Long certId = entry.getKey();
            String certName = certIdToName.getOrDefault(certId, "Unknown");
            
            List<DashboardResponse.TrendPoint> points = entry.getValue().entrySet().stream()
                    .map(dateEntry -> {
                        String dateStr = dateEntry.getKey();
                        List<UserQuizHistory> list = dateEntry.getValue();
                        long correctCount = list.stream().filter(h -> Boolean.TRUE.equals(h.getIsCorrect())).count();
                        float accuracy = list.isEmpty() ? 0f : (float) correctCount / list.size() * 100f;
                        return new DashboardResponse.TrendPoint(dateStr, accuracy);
                    })
                    .sorted(Comparator.comparing(DashboardResponse.TrendPoint::date))
                    .collect(Collectors.toList());
            
            certTrends.add(new DashboardResponse.CertTrend(certName, points));
        }

        List<DashboardResponse.CertInfo> allCertInfos = certifications.stream()
                .map(c -> new DashboardResponse.CertInfo(c.getId(), c.getCertName()))
                .collect(Collectors.toList());

        return new DashboardResponse(
                certCount,
                scrapCount,
                scrapItems,
                studyTimeStr,
                String.format("%.0f%%", avgCorrectRate),
                heatmapData,
                targetExam,
                certStats,
                certTrends,
                allCertInfos
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

    @Override
    @Transactional
    public void addSchedule(String email, com.certimate.manager.user.dto.AddScheduleRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        com.certimate.manager.user.entity.Certification cert = certificationRepository.findById(request.certId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "자격증을 찾을 수 없습니다."));

        com.certimate.manager.user.entity.ExamSchedule schedule = com.certimate.manager.user.entity.ExamSchedule.builder()
                .user(user)
                .certification(cert)
                .qualName(cert.getCertName())
                .examType(request.examType())
                .examDate(request.examDate())
                .targetReadCount(request.targetReadCount() != null && request.targetReadCount() > 0 ? request.targetReadCount() : 1)
                .build();

        examScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(String email) {
        // Since EXAM_SCHEDULE is global but used per cert, we will find the most recent user learn log
        // and delete the schedules for that certification to simulate "deleting the user's target exam".
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<com.certimate.manager.user.entity.UserLearnLog> logs = userLearnLogRepository.findByUser_Id(user.getId());
        if (!logs.isEmpty()) {
            logs.sort((a, b) -> {
                if (a.getLastStudiedAt() == null && b.getLastStudiedAt() == null) return 0;
                if (a.getLastStudiedAt() == null) return 1;
                if (b.getLastStudiedAt() == null) return -1;
                return b.getLastStudiedAt().compareTo(a.getLastStudiedAt());
            });
            com.certimate.manager.user.entity.UserLearnLog recentLog = logs.get(0);
            List<com.certimate.manager.user.entity.ExamSchedule> schedules = examScheduleRepository.findByCertification_IdAndExamDateAfterOrderByExamDateAsc(
                    recentLog.getCertification().getId(), LocalDate.now().minusDays(1));
            
            if (!schedules.isEmpty()) {
                examScheduleRepository.deleteAll(schedules);
            }
        }
    }
}
