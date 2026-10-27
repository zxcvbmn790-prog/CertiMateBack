package com.certimate.manager.user.service;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.user.entity.ExamSchedule;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoNotificationService {

    private final UserRepository userRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    /**
     * 서버 기동 시 users 테이블에 카카오톡 알림용 컬럼이 없으면 안전하게 추가합니다.
     */
    @PostConstruct
    public void ensureUserKakaoColumns() {
        addColumnIfNotExists("kakao_access_token", "VARCHAR(512) NULL");
        addColumnIfNotExists("kakao_refresh_token", "VARCHAR(512) NULL");
        addColumnIfNotExists("kakao_notify_enabled", "TINYINT(1) DEFAULT 1");
        addColumnIfNotExists("kakao_notify_days", "VARCHAR(100) DEFAULT '7,1'");
        addColumnIfNotExists("kakao_notify_time", "VARCHAR(10) DEFAULT '09:00'");
    }

    private void addColumnIfNotExists(String columnName, String columnDef) {
        try {
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SHOW COLUMNS FROM users LIKE ?", columnName);
            if (cols.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN " + columnName + " " + columnDef);
                log.info("[KakaoNotificationService] Added column '{}' to users table.", columnName);
            }
        } catch (Exception e) {
            log.warn("[KakaoNotificationService] Column check/add skipped for {}: {}", columnName, e.getMessage());
        }
    }

    /**
     * 현재 로그인한 사용자의 카카오톡 알림 연동 상태 및 주기/시간 설정을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        boolean connected = (user.getKakaoRefreshToken() != null && !user.getKakaoRefreshToken().isBlank())
                || (user.getKakaoAccessToken() != null && !user.getKakaoAccessToken().isBlank());
        boolean enabled = user.getKakaoNotifyEnabled() != null ? user.getKakaoNotifyEnabled() : false;
        String notifyDays = (user.getKakaoNotifyDays() != null && !user.getKakaoNotifyDays().isBlank())
                ? user.getKakaoNotifyDays() : "7,1";
        String notifyTime = (user.getKakaoNotifyTime() != null && !user.getKakaoNotifyTime().isBlank())
                ? user.getKakaoNotifyTime() : "09:00";

        List<ExamSchedule> upcoming = examScheduleRepository
                .findByUser_IdAndExamDateAfterOrderByExamDateAsc(user.getId(), LocalDate.now().minusDays(1));

        Map<String, Object> result = new HashMap<>();
        result.put("connected", connected);
        result.put("enabled", enabled);
        result.put("notifyDays", notifyDays);
        result.put("notifyTime", notifyTime);
        result.put("scheduleCount", upcoming.size());
        return result;
    }

    /**
     * 카카오 인가 코드(code)와 사용자가 선택한 알림 주기/시간을 받아 연동하고,
     * 연동 즉시 사용자의 시험 일정 안내 카톡을 발신합니다!
     */
    @Transactional
    public void connectKakao(String email, String code, String notifyDays, String notifyTime) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", kakaoClientId);
            body.add("redirect_uri", kakaoRedirectUri);
            body.add("code", code);

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, headers);
            ResponseEntity<String> tokenResponse = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token", HttpMethod.POST, tokenRequest, String.class);

            JsonNode tokenNode = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenNode.get("access_token").asText();
            String refreshToken = tokenNode.has("refresh_token") && !tokenNode.get("refresh_token").isNull()
                    ? tokenNode.get("refresh_token").asText() : null;

            user.updateKakaoTokens(accessToken, refreshToken);
            user.updateKakaoNotifySettings(
                    true,
                    (notifyDays != null && !notifyDays.isBlank()) ? notifyDays : user.getKakaoNotifyDays(),
                    (notifyTime != null && !notifyTime.isBlank()) ? notifyTime : user.getKakaoNotifyTime()
            );
            userRepository.save(user);
        } catch (Exception e) {
            log.error("[KakaoNotificationService] 카카오 알림 연동 실패", e);
            throw new CustomException(HttpStatus.BAD_REQUEST, "카카오톡 알림 연동에 실패했습니다: " + e.getMessage());
        }

        // 권한 동의 및 연동 완료 즉시 시험 일정 카카오톡 발신!
        sendImmediateScheduleNotification(email);
    }

    /**
     * 자동 알림 설정(ON/OFF, 알림 주기, 알림 시간)을 저장합니다.
     */
    @Transactional
    public void updateSettings(String email, Boolean enabled, String notifyDays, String notifyTime) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.updateKakaoNotifySettings(enabled, notifyDays, notifyTime);
        userRepository.save(user);
    }

    /**
     * 사용자의 다가오는 시험 일정과 설정된 알림 주기를 카카오톡 '나에게 보내기'로 즉시 발송합니다.
     */
    @Transactional
    public void sendImmediateScheduleNotification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if ((user.getKakaoAccessToken() == null || user.getKakaoAccessToken().isBlank())
                && (user.getKakaoRefreshToken() == null || user.getKakaoRefreshToken().isBlank())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "먼저 카카오톡 알림 연동을 진행해주세요.");
        }

        List<ExamSchedule> schedules = examScheduleRepository
                .findByUser_IdAndExamDateAfterOrderByExamDateAsc(user.getId(), LocalDate.now().minusDays(1));

        String daysLabel = formatDaysLabel(user.getKakaoNotifyDays());
        String timeLabel = (user.getKakaoNotifyTime() != null && !user.getKakaoNotifyTime().isBlank())
                ? user.getKakaoNotifyTime() : "09:00";

        String message;
        if (schedules.isEmpty()) {
            message = String.format(
                    "📅 [CertiMate 시험 자동 알림 설정 완료]\n\n%s님, 카카오톡 자동 알림이 활성화되었습니다!\n• 알림 주기: %s\n• 알림 시각: %s\n\n내 정보에서 목표 시험 일정을 등록하시면 맞춰서 카톡을 보내드립니다. 🔥",
                    user.getName(), daysLabel, timeLabel
            );
        } else {
            ExamSchedule nextExam = schedules.get(0);
            String certName = nextExam.getCertification() != null
                    ? nextExam.getCertification().getCertName()
                    : (nextExam.getQualName() != null ? nextExam.getQualName() : "자격증 시험");
            long dDay = ChronoUnit.DAYS.between(LocalDate.now(), nextExam.getExamDate());
            String dDayStr = dDay <= 0 ? "D-Day" : "D-" + dDay;

            StringBuilder sb = new StringBuilder();
            sb.append("📅 [CertiMate 시험 일정 알림]\n\n");
            sb.append(String.format("%s님의 준비 중인 시험 일정입니다.\n", user.getName()));
            sb.append(String.format("• 종목: %s (%s)\n", certName, nextExam.getExamType()));
            sb.append(String.format("• 시험일: %s (%s)\n", nextExam.getExamDate(), dDayStr));
            if (schedules.size() > 1) {
                sb.append(String.format("• 외 %d건의 시험 일정\n", schedules.size() - 1));
            }
            sb.append(String.format("\n⏰ 자동 알림: %s (%s)", daysLabel, timeLabel));
            message = sb.toString();
        }

        sendKakaoMemo(user, message);
    }

    private String formatDaysLabel(String notifyDays) {
        if (notifyDays == null || notifyDays.isBlank()) return "D-7, D-1";
        return Arrays.stream(notifyDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> "0".equals(s) ? "D-Day" : "D-" + s)
                .collect(Collectors.joining(", "));
    }

    /**
     * 매 분 0초마다 실행되어, 사용자가 직접 설정한 알림 시각(HH:mm)과 알림 주기(D-N)가 일치하는 시험 일정에 대해 카카오톡을 자동 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendScheduledExamNotifications() {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kst);
        String currentTimeStr = LocalTime.now(kst).format(DateTimeFormatter.ofPattern("HH:mm"));

        List<ExamSchedule> activeSchedules = examScheduleRepository.findAllActiveUserSchedules(today);
        for (ExamSchedule schedule : activeSchedules) {
            User user = schedule.getUser();
            if (user == null) continue;
            if (user.getKakaoNotifyEnabled() == null || !user.getKakaoNotifyEnabled()) continue;

            String userTime = (user.getKakaoNotifyTime() != null && !user.getKakaoNotifyTime().isBlank())
                    ? user.getKakaoNotifyTime() : "09:00";
            if (!currentTimeStr.equals(userTime)) continue;

            long daysLeft = ChronoUnit.DAYS.between(today, schedule.getExamDate());
            Set<Long> targetDays = parseNotifyDays(user.getKakaoNotifyDays());

            if (targetDays.contains(daysLeft)) {
                sendScheduledAlertForExam(schedule, (int) daysLeft);
            }
        }
    }

    private Set<Long> parseNotifyDays(String notifyDays) {
        String raw = (notifyDays != null && !notifyDays.isBlank()) ? notifyDays : "7,1";
        Set<Long> set = new HashSet<>();
        for (String part : raw.split(",")) {
            try {
                set.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return set;
    }

    private void sendScheduledAlertForExam(ExamSchedule schedule, int daysLeft) {
        User user = schedule.getUser();
        if ((user.getKakaoAccessToken() == null || user.getKakaoAccessToken().isBlank())
                && (user.getKakaoRefreshToken() == null || user.getKakaoRefreshToken().isBlank())) {
            return;
        }

        String certName = schedule.getCertification() != null
                ? schedule.getCertification().getCertName()
                : (schedule.getQualName() != null ? schedule.getQualName() : "자격증 시험");

        String text;
        if (daysLeft == 0) {
            text = String.format(
                    "🔥 [CertiMate 시험 당일(D-Day) 알림]\n\n%s님, 오늘은 '%s (%s)' 시험 당일입니다!\n\n• 시험일: %s\n• 수험표와 신분증을 꼭 지참하시고, 그동안 준비한 실력을 마음껏 발휘하세요! 합격을 응원합니다! 🎉",
                    user.getName(), certName, schedule.getExamType(), schedule.getExamDate()
            );
        } else if (daysLeft == 1) {
            text = String.format(
                    "⏰ [CertiMate 시험 전날(D-1) 알림]\n\n%s님, 내일은 '%s (%s)' 시험일입니다!\n\n• 시험일: %s\n• 수험표와 신분증을 미리 챙기시고 고사장 위치를 확인하세요. 합격을 응원합니다! 🎉",
                    user.getName(), certName, schedule.getExamType(), schedule.getExamDate()
            );
        } else {
            text = String.format(
                    "📅 [CertiMate 시험 D-%d 알림]\n\n%s님, 준비 중인 '%s (%s)' 시험이 %d일 남았습니다!\n\n• 시험일: %s\n• 목표 회독: %d회독\n\n오답노트와 모의고사로 실전 감각을 점검해 보세요! 🔥",
                    daysLeft, user.getName(), certName, schedule.getExamType(), daysLeft, schedule.getExamDate(),
                    schedule.getTargetReadCount() != null ? schedule.getTargetReadCount() : 1
            );
        }

        try {
            sendKakaoMemo(user, text);
            log.info("[KakaoNotificationService] D-{} 카카오톡 자동 알림 발송 완료 (user={}, cert={})", daysLeft, user.getEmail(), certName);
        } catch (Exception e) {
            log.error("[KakaoNotificationService] D-{} 카카오톡 자동 알림 발송 실패 (user={}): {}", daysLeft, user.getEmail(), e.getMessage());
        }
    }

    /**
     * 카카오 '나에게 보내기' API 호출 (토큰 만료 시 refresh_token으로 자동 갱신 후 재시도)
     */
    private void sendKakaoMemo(User user, String text) {
        String accessToken = user.getKakaoAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = refreshKakaoAccessToken(user);
        }

        try {
            doSendKakaoMemo(accessToken, text);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("[KakaoNotificationService] 카카오 액세스 토큰 만료 감지 -> 토큰 갱신 후 재시도 (user={})", user.getEmail());
            String newAccessToken = refreshKakaoAccessToken(user);
            try {
                doSendKakaoMemo(newAccessToken, text);
            } catch (Exception retryEx) {
                log.error("[KakaoNotificationService] 토큰 갱신 후 재시도 실패", retryEx);
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "카카오톡 메시지 발송 실패: " + retryEx.getMessage());
            }
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("[KakaoNotificationService] 카카오톡 메시지 전송 권한 부족: {}", e.getResponseBodyAsString());
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "카카오톡 메시지 전송(talk_message) 동의가 필요합니다. 권한 동의 후 다시 시도해주세요.");
        } catch (Exception e) {
            log.error("[KakaoNotificationService] 카카오톡 메시지 전송 오류", e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "카카오톡 메시지 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void doSendKakaoMemo(String accessToken, String text) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        String safeText = text.length() > 195 ? text.substring(0, 192) + "..." : text;

        Map<String, Object> linkObj = new HashMap<>();
        linkObj.put("web_url", "http://localhost:3000/calendar");
        linkObj.put("mobile_web_url", "http://localhost:3000/calendar");

        Map<String, Object> templateObj = new HashMap<>();
        templateObj.put("object_type", "text");
        templateObj.put("text", safeText);
        templateObj.put("link", linkObj);
        templateObj.put("button_title", "시험 일정 및 고사장 확인");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("template_object", objectMapper.writeValueAsString(templateObj));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        restTemplate.exchange(
                "https://kapi.kakao.com/v2/api/talk/memo/default/send",
                HttpMethod.POST,
                request,
                String.class
        );
    }

    private String refreshKakaoAccessToken(User user) {
        if (user.getKakaoRefreshToken() == null || user.getKakaoRefreshToken().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "카카오 토큰이 만료되었습니다. 다시 카카오톡 알림 연동을 진행해주세요.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", kakaoClientId);
            body.add("refresh_token", user.getKakaoRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token", HttpMethod.POST, request, String.class);

            JsonNode node = objectMapper.readTree(response.getBody());
            String newAccessToken = node.get("access_token").asText();
            String newRefreshToken = node.has("refresh_token") && !node.get("refresh_token").isNull()
                    ? node.get("refresh_token").asText() : null;

            user.updateKakaoTokens(newAccessToken, newRefreshToken);
            userRepository.save(user);
            return newAccessToken;
        } catch (Exception e) {
            log.error("[KakaoNotificationService] 카카오 토큰 갱신 실패", e);
            throw new CustomException(HttpStatus.BAD_REQUEST, "카카오 인증이 만료되었습니다. 다시 카카오톡 연동을 진행해주세요.");
        }
    }
}
