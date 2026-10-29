package com.certimate.manager.job;

import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.config.SecurityConfig;
import com.certimate.manager.job.controller.JobController;
import com.certimate.manager.job.dto.JobMatchingResponse;
import com.certimate.manager.job.service.JobMatchingService;
import com.certimate.manager.security.JwtProvider;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.certimate.manager.user.repository.UserCertificationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 취업연계는 호출마다 사람인 페이지를 조회한다. 봇이 반복 호출해 차단당하지 않도록
// 비로그인은 막고, 같은 검색은 캐시로 사람인에 한 번만 요청해야 한다.
class JobRecommendationGuardTest {

    @Nested
    @WebMvcTest(JobController.class)
    @Import(SecurityConfig.class)
    class Auth {
        @Autowired MockMvc mvc;
        @MockBean JobMatchingService jobMatchingService;
        @MockBean JwtProvider jwtProvider;

        @Test
        void anonymousIsRejected() throws Exception {
            mvc.perform(get("/api/jobs/recommendations")).andExpect(status().isUnauthorized());
            verifyNoInteractions(jobMatchingService);
        }
    }

    private static final String HTML = """
            <div class="item_recruit" value="123">
              <h2 class="job_tit"><a href="#" title="백엔드 개발자 (정보처리기사 우대)">x</a></h2>
              <strong class="corp_name"> <a href="#">(주)테스트</a></strong>
              <div class="job_date"> <span class="date">~ 11/30(일)</span></div>
              <div class="job_condition"><span>서울</span><span>신입</span><span>대졸</span><span>정규직</span></div>
            </div>
            <div class="pagination"></div>
            """;

    private JobMatchingService newService(UserRepository users) {
        return new JobMatchingService(users, mock(UserCertificationRepository.class), mock(ExamScheduleRepository.class));
    }

    @Test
    void sameSearchHitsSaraminOnce() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        JobMatchingService service = newService(users);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer saramin = MockRestServiceServer.bindTo(restTemplate).build();
        saramin.expect(once(), requestTo(org.hamcrest.Matchers.containsString("saramin.co.kr")))
                .andRespond(withSuccess(HTML, MediaType.TEXT_HTML));

        JobMatchingResponse first = service.getJobRecommendations("u@test.com", "정보처리기사", "전체", 1);
        JobMatchingResponse second = service.getJobRecommendations("u@test.com", "정보처리기사", "전체", 1);

        assertThat(first.getPostings()).hasSize(1);
        assertThat(second.getPostings()).extracting("id").containsExactly("123");
        saramin.verify();
    }

    @Test
    void emptyResultIsNotCached() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        JobMatchingService service = newService(users);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer saramin = MockRestServiceServer.bindTo(restTemplate).build();
        saramin.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("saramin.co.kr")))
                .andRespond(withSuccess("<html>blocked</html>", MediaType.TEXT_HTML));

        service.getJobRecommendations("u@test.com", "정보처리기사", "전체", 1);
        service.getJobRecommendations("u@test.com", "정보처리기사", "전체", 1);

        saramin.verify();
    }

    @Test
    void junkKeywordIsNormalizedToKnownCertName() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        JobMatchingService service = newService(users);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer saramin = MockRestServiceServer.bindTo(restTemplate).build();
        // 사람인에는 입력한 아무 말이 아니라 정식 자격증 이름으로 검색해야 한다
        saramin.expect(once(), requestTo(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%E3%85%8B"))))
                .andRespond(withSuccess(HTML, MediaType.TEXT_HTML));

        JobMatchingResponse res = service.getJobRecommendations("u@test.com", "정보처리기사ㅋㅋㅋ", "전체", 1);

        assertThat(res.getActiveKeyword()).isEqualTo("정보처리기사");
        assertThat(res.getPostings()).extracting("matchedCert").containsOnly("정보처리기사");
        saramin.verify();
    }
}
