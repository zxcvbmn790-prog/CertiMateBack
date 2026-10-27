package com.certimate.manager.exam;

import com.certimate.manager.config.SecurityConfig;
import com.certimate.manager.exam.controller.ExamController;
import com.certimate.manager.exam.service.ExamService;
import com.certimate.manager.security.JwtProvider;
import com.certimate.manager.user.repository.CertificationRepository;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AI 해설 생성·신고는 로그인 필수, 나머지 /api/exams 조회는 계속 공개여야 한다.
@WebMvcTest(ExamController.class)
@Import(SecurityConfig.class)
class ExplanationSecurityTest {

    @Autowired MockMvc mvc;
    @MockBean ExamService examService;
    @MockBean ExamScheduleRepository examScheduleRepository;
    @MockBean CertificationRepository certificationRepository;
    @MockBean JwtProvider jwtProvider;

    @Test
    void anonymousCannotGenerateOrReport() throws Exception {
        mvc.perform(post("/api/exams/explanations").contentType(MediaType.APPLICATION_JSON).content("[1]"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/exams/1/report-explanation"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(examService);
    }

    @Test
    void loggedInUserCanReport() throws Exception {
        when(jwtProvider.validateToken("ok")).thenReturn(true);
        when(jwtProvider.getEmailFromToken("ok")).thenReturn("u@test.com");
        mvc.perform(post("/api/exams/1/report-explanation").cookie(new Cookie("accessToken", "ok")))
                .andExpect(status().isOk());
        verify(examService).reportExplanation(1L);
    }

    @Test
    void mockExamStaysPublic() throws Exception {
        mvc.perform(get("/api/exams/1/mock")).andExpect(status().isOk());
    }
}
