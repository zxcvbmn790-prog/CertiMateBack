package com.certimate.manager.user;

import com.certimate.manager.auth.entity.User;
import com.certimate.manager.auth.repository.UserRepository;
import com.certimate.manager.exam.repository.AiLearnRepository;
import com.certimate.manager.exam.repository.UserQuizHistoryRepository;
import com.certimate.manager.exception.CustomException;
import com.certimate.manager.user.dto.AddAcquiredCertRequest;
import com.certimate.manager.user.repository.CertificationRepository;
import com.certimate.manager.user.repository.ExamScheduleRepository;
import com.certimate.manager.user.repository.UserCertificationRepository;
import com.certimate.manager.user.repository.UserLearnLogRepository;
import com.certimate.manager.user.repository.UserScrapRepository;
import com.certimate.manager.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 자격증을 직접 입력해도 공용 자격증 목록에 새 행을 만들면 안 된다(임의 문자열로 목록이 오염됨).
class CertificationInputGuardTest {

    @Test
    void unknownCertNameIsRejectedAndNotCreated() {
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certs = mock(CertificationRepository.class);
        UserCertificationRepository userCerts = mock(UserCertificationRepository.class);
        UserServiceImpl service = new UserServiceImpl(users, userCerts, mock(UserScrapRepository.class),
                mock(UserLearnLogRepository.class), mock(ExamScheduleRepository.class), mock(UserQuizHistoryRepository.class),
                mock(AiLearnRepository.class), certs);
        User me = mock(User.class);
        when(me.getId()).thenReturn(1L);
        when(users.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(certs.findByCertName("아무말자격증ㅋㅋ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAcquiredCertification("u@test.com", new AddAcquiredCertRequest(null, "아무말자격증ㅋㅋ", null, null)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("목록에 없는 자격증");
        verify(certs, never()).save(any());
        verify(userCerts, never()).save(any());
    }
}
