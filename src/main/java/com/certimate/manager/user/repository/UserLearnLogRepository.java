package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.UserLearnLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserLearnLogRepository extends JpaRepository<UserLearnLog, Long> {
    List<UserLearnLog> findByUser_Id(Long userId);
    void deleteByUser_Id(Long userId);
}
