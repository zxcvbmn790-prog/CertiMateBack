package com.certimate.manager.job.service;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.job.dto.JobMatchingResponse;
import com.certimate.manager.job.dto.JobPostingDto;
import com.certimate.manager.user.entity.ExamSchedule;
import com.certimate.manager.user.entity.UserCertification;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.certimate.manager.user.repository.UserCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final UserRepository userRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 같은 검색어·페이지는 30분간 사람인을 다시 조회하지 않는다 (차단·부하 방지).
    // ponytail: 단일 인스턴스 인메모리 캐시. 키 종류가 많아지면 크기 제한 있는 캐시(Caffeine)로 바꿀 것.
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_PAGE = 5;
    private static final int MAX_CACHE_ENTRIES = 500;
    private final Map<String, CachedPostings> postingsCache = new ConcurrentHashMap<>();

    private record CachedPostings(List<JobPostingDto> postings, Instant fetchedAt) {
        boolean isFresh() {
            return fetchedAt.isAfter(Instant.now().minus(CACHE_TTL));
        }
    }

    private static final List<String> KNOWN_CERTS = List.of(
            "정보처리기사", "정보처리산업기사", "SQLD", "ADsP", "리눅스마스터",
            "네트워크관리사", "빅데이터분석기사", "정보보안기사", "컴퓨터활용능력", "전기기사"
    );

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<div class=\"item_recruit\"\\s+value=\"(\\d+)\"[\\s\\S]*?(?=<div class=\"item_recruit\"\\s+value=\"|<div class=\"pagination\"|$)"
    );
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<h2 class=\"job_tit\">[\\s\\S]*?<a[^>]*title=\"([^\"]+)\""
    );
    private static final Pattern CORP_PATTERN = Pattern.compile(
            "<strong class=\"corp_name\">\\s*<a[^>]*>([\\s\\S]*?)</a>"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "<div class=\"job_date\">\\s*<span class=\"date\">([\\s\\S]*?)</span>"
    );
    private static final Pattern CONDITION_BLOCK_PATTERN = Pattern.compile(
            "<div class=\"job_condition\">([\\s\\S]*?)</div>"
    );
    private static final Pattern SPAN_PATTERN = Pattern.compile(
            "<span[^>]*>([\\s\\S]*?)</span>"
    );
    private static final Pattern SECTOR_BLOCK_PATTERN = Pattern.compile(
            "<div class=\"job_sector\">([\\s\\S]*?)</div>"
    );
    private static final Pattern LINK_TEXT_PATTERN = Pattern.compile(
            "<a[^>]*>([\\s\\S]*?)</a>"
    );
    private static final Pattern BADGE_PATTERN = Pattern.compile(
            "<div class=\"area_badge\">[\\s\\S]*?<span class=\"badge[^\"]*\"[^>]*>([\\s\\S]*?)</span>"
    );

    @Transactional(readOnly = true)
    public JobMatchingResponse getJobRecommendations(String email, String keyword, String roleFilter, int page) {
        String userName = null;
        String userMajor = null;
        String userInterest = null;
        Set<String> myCertSet = new LinkedHashSet<>();
        Set<String> acquiredCertSet = new LinkedHashSet<>();
        Set<String> targetCertSet = new LinkedHashSet<>();

        if (email != null && !email.isBlank()) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userName = user.getName();
                userMajor = user.getMajor();
                userInterest = user.getInterest();

                // 1. 보유 자격증 조회
                List<UserCertification> owned = userCertificationRepository.findByUser_Id(user.getId());
                for (UserCertification uc : owned) {
                    if (uc.getCertification() != null && uc.getCertification().getCertName() != null) {
                        String name = uc.getCertification().getCertName().trim();
                        myCertSet.add(name);
                        acquiredCertSet.add(name);
                    }
                }

                // 2. 준비 중인 시험 일정 자격증 조회
                List<ExamSchedule> schedules = examScheduleRepository
                        .findByUser_IdAndExamDateAfterOrderByExamDateAsc(user.getId(), LocalDate.now().minusDays(1));
                for (ExamSchedule es : schedules) {
                    String name = null;
                    if (es.getCertification() != null && es.getCertification().getCertName() != null) {
                        name = es.getCertification().getCertName().trim();
                    } else if (es.getQualName() != null && !es.getQualName().isBlank()) {
                        name = es.getQualName().trim();
                    }
                    if (name != null) {
                        myCertSet.add(name);
                        targetCertSet.add(name);
                    }
                }
            }
        }

        List<String> myCertifications = new ArrayList<>(myCertSet);

        // 검색할 기준 자격증 결정 (유효한 자격증 명칭인지 검증)
        String baseCertKeyword;
        if (keyword != null && !keyword.isBlank() && !"AUTO".equalsIgnoreCase(keyword)) {
            String trimmed = keyword.trim();
            boolean isValid = myCertifications.stream().anyMatch(c -> c.equalsIgnoreCase(trimmed) || c.contains(trimmed) || trimmed.contains(c))
                    || KNOWN_CERTS.stream().anyMatch(c -> c.equalsIgnoreCase(trimmed) || c.contains(trimmed) || trimmed.contains(c));
            if (isValid) {
                baseCertKeyword = trimmed;
            } else if (!myCertifications.isEmpty()) {
                baseCertKeyword = myCertifications.get(0);
            } else {
                baseCertKeyword = "정보처리기사";
            }
        } else if (!myCertifications.isEmpty()) {
            baseCertKeyword = myCertifications.get(0);
        } else {
            baseCertKeyword = "정보처리기사";
        }

        String searchQuery = baseCertKeyword;
        if (roleFilter != null && !roleFilter.isBlank() && !"전체".equals(roleFilter)) {
            searchQuery = baseCertKeyword + " " + roleFilter.trim();
        }

        boolean isMyCert = myCertifications.stream()
                .anyMatch(c -> c.contains(baseCertKeyword) || baseCertKeyword.contains(c));

        List<JobPostingDto> postings = cachedSaraminPostings(searchQuery, baseCertKeyword, isMyCert, Math.min(Math.max(1, page), MAX_PAGE));

        return JobMatchingResponse.builder()
                .userName(userName)
                .userMajor(userMajor)
                .userInterest(userInterest)
                .myCertifications(myCertifications)
                .acquiredCertifications(new ArrayList<>(acquiredCertSet))
                .targetCertifications(new ArrayList<>(targetCertSet))
                .activeKeyword(baseCertKeyword)
                .postings(postings)
                .build();
    }

    private List<JobPostingDto> cachedSaraminPostings(String searchQuery, String certBadgeName, boolean isMyCert, int page) {
        // isMyCert는 점수 계산에 쓰이므로 키에 포함한다.
        String key = searchQuery + "|" + certBadgeName + "|" + isMyCert + "|" + page;
        CachedPostings hit = postingsCache.get(key);
        if (hit != null && hit.isFresh()) {
            return hit.postings();
        }
        List<JobPostingDto> postings = fetchSaraminPostings(searchQuery, certBadgeName, isMyCert, page);
        // 실패·차단 시 빈 목록이 오므로 빈 결과는 저장하지 않는다 (다음 요청에서 다시 시도).
        if (!postings.isEmpty()) {
            if (postingsCache.size() >= MAX_CACHE_ENTRIES) {
                postingsCache.entrySet().removeIf(e -> !e.getValue().isFresh());
                if (postingsCache.size() >= MAX_CACHE_ENTRIES) {
                    postingsCache.clear();
                }
            }
            postingsCache.put(key, new CachedPostings(List.copyOf(postings), Instant.now()));
        }
        return postings;
    }

    private List<JobPostingDto> fetchSaraminPostings(String searchQuery, String certBadgeName, boolean isMyCert, int page) {
        List<JobPostingDto> list = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String urlStr = "https://www.saramin.co.kr/zf_user/search/recruit?searchword=" + encoded
                    + "&recruitPage=" + page
                    + "&recruitSort=relation&recruitPageCount=18";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(urlStr), HttpMethod.GET, entity, String.class);

            String html = response.getBody();
            if (html == null || html.isBlank()) {
                return list;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(html);
            int idx = 0;
            while (itemMatcher.find() && idx < 18) {
                String recIdx = itemMatcher.group(1);
                String block = itemMatcher.group(0);

                String title = extractSingle(TITLE_PATTERN, block);
                String company = extractSingle(CORP_PATTERN, block);
                if (title.isEmpty() || company.isEmpty()) continue;

                String deadline = extractSingle(DATE_PATTERN, block);
                String badge = extractSingle(BADGE_PATTERN, block);

                // 근무 조건 파싱 (지역, 경력, 학력, 고용형태, 연봉)
                List<String> conds = new ArrayList<>();
                String condBlock = extractRawSingle(CONDITION_BLOCK_PATTERN, block);
                if (!condBlock.isEmpty()) {
                    Matcher spanMatcher = SPAN_PATTERN.matcher(condBlock);
                    while (spanMatcher.find()) {
                        String cleaned = cleanHtml(spanMatcher.group(1));
                        if (!cleaned.isEmpty()) {
                            conds.add(cleaned);
                        }
                    }
                }

                String location = conds.size() > 0 ? conds.get(0) : "전국";
                String experience = conds.size() > 1 ? conds.get(1) : "경력무관";
                String education = conds.size() > 2 ? conds.get(2) : "학력무관";
                String employmentType = conds.size() > 3 ? conds.get(3) : "정규직";
                String salary = conds.size() > 4 ? conds.get(4) : "";

                // 직무 분야 태그 파싱
                List<String> sectors = new ArrayList<>();
                String sectorBlock = extractRawSingle(SECTOR_BLOCK_PATTERN, block);
                if (!sectorBlock.isEmpty()) {
                    Matcher linkMatcher = LINK_TEXT_PATTERN.matcher(sectorBlock);
                    while (linkMatcher.find() && sectors.size() < 5) {
                        String sec = cleanHtml(linkMatcher.group(1));
                        if (!sec.isEmpty()) {
                            sectors.add(sec);
                        }
                    }
                }

                // 매칭 점수 산출 (보유 자격증 여부 + 신입/경력무관 우대 여부)
                int baseScore = isMyCert ? 92 : 82;
                if (experience.contains("신입") || experience.contains("무관")) {
                    baseScore += 5;
                }
                if (title.contains(certBadgeName)) {
                    baseScore += 2;
                }
                int finalScore = Math.min(99, baseScore - (idx % 5));

                list.add(JobPostingDto.builder()
                        .id(recIdx)
                        .title(title)
                        .companyName(company)
                        .location(location)
                        .experience(experience)
                        .education(education)
                        .employmentType(employmentType)
                        .salary(salary)
                        .deadline(deadline)
                        .sectors(sectors)
                        .badge(badge)
                        .url("https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=" + recIdx)
                        .matchedCert(certBadgeName)
                        .isMyCert(isMyCert)
                        .matchScore(finalScore)
                        .build());

                idx++;
            }
        } catch (Exception e) {
            log.error("[JobMatchingService] 사람인 채용공고 실시간 조회 오류 (query={}): {}", searchQuery, e.getMessage());
        }
        return list;
    }

    private String extractRawSingle(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private String extractSingle(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? cleanHtml(m.group(1)) : "";
    }

    private String cleanHtml(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("<[^>]*>", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
