package com.certimate.manager.exam;

import com.certimate.manager.exam.dto.AiLearnResponse;
import com.certimate.manager.exam.entity.AiLearn;
import com.certimate.manager.exam.repository.AiLearnRepository;
import com.certimate.manager.exam.repository.UserQuizHistoryRepository;
import com.certimate.manager.exam.service.AiExplanationService;
import com.certimate.manager.exam.service.impl.ExamServiceImpl;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.user.entity.Certification;
import com.certimate.manager.user.repository.CertificationRepository;
import com.certimate.manager.user.repository.UserLearnLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// 모의고사는 자격증별 실제 시험 구성(과목 수 × 과목별 문항 수)대로 출제해야 한다.
class MockExamBlueprintTest {

    private final AiLearnRepository aiLearnRepository = mock(AiLearnRepository.class);
    private final CertificationRepository certificationRepository = mock(CertificationRepository.class);
    private final ExamServiceImpl service = new ExamServiceImpl(aiLearnRepository, mock(UserQuizHistoryRepository.class),
            mock(UserRepository.class), mock(UserLearnLogRepository.class), certificationRepository, mock(AiExplanationService.class));

    private static List<AiLearn> bank(long certId, int subject, int size) {
        List<AiLearn> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            AiLearn q = new AiLearn();
            ReflectionTestUtils.setField(q, "learnId", certId * 10000 + subject * 1000L + i);
            ReflectionTestUtils.setField(q, "certId", certId);
            ReflectionTestUtils.setField(q, "subjectNum", subject);
            list.add(q);
        }
        return list;
    }

    private void cert(long id, String name) {
        when(certificationRepository.findById(id)).thenReturn(Optional.of(Certification.builder().certName(name).build()));
    }

    private Map<Integer, Long> countBySubject(List<AiLearnResponse> exam) {
        return exam.stream().collect(Collectors.groupingBy(AiLearnResponse::subjectNum, Collectors.counting()));
    }

    @Test
    void infoEngineerUsesFiveSubjectsOfTwenty() {
        cert(2L, "정보처리기사");
        for (int s = 1; s <= 5; s++) when(aiLearnRepository.findByCertIdAndSubjectNum(2L, s)).thenReturn(bank(2, s, 40));

        List<AiLearnResponse> exam = service.generateMockExam(2L);

        assertThat(exam).hasSize(100);
        assertThat(countBySubject(exam)).containsExactlyInAnyOrderEntriesOf(Map.of(1, 20L, 2, 20L, 3, 20L, 4, 20L, 5, 20L));
        assertThat(exam).extracting(AiLearnResponse::learnId).doesNotHaveDuplicates();
    }

    @Test
    void sqldUsesUnevenSubjectCountsAndTakesWhatExistsWhenShort() {
        cert(8L, "SQL 개발자 (SQLD)");
        when(aiLearnRepository.findByCertIdAndSubjectNum(8L, 1)).thenReturn(bank(8, 1, 30));
        when(aiLearnRepository.findByCertIdAndSubjectNum(8L, 2)).thenReturn(bank(8, 2, 25)); // 40문제보다 적음

        assertThat(countBySubject(service.generateMockExam(8L))).containsExactlyInAnyOrderEntriesOf(Map.of(1, 10L, 2, 25L));
    }

    @Test
    void unknownCertKeepsThreeSubjectsOfTwenty() {
        cert(99L, "새로운 자격증");
        when(aiLearnRepository.findByCertIdAndSubjectNum(eq(99L), anyInt())).thenAnswer(inv -> bank(99, inv.getArgument(1), 30));

        List<AiLearnResponse> exam = service.generateMockExam(99L);

        assertThat(exam).hasSize(60);
        verify(aiLearnRepository, never()).findByCertIdAndSubjectNum(99L, 4);
    }
}
