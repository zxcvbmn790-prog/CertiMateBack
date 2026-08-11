package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.UserCertification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCertificationRepository extends JpaRepository<UserCertification, Long> {
    int countByUser_Id(Long userId);
    void deleteByUser_Id(Long userId);
}
