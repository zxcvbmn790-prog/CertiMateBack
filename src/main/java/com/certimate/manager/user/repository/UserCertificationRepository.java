package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.UserCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCertificationRepository extends JpaRepository<UserCertification, Long> {
    List<UserCertification> findByUser_Id(Long userId);
    int countByUser_Id(Long userId);
    void deleteByUser_Id(Long userId);
}
