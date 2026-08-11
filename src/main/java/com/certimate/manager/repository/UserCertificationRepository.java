package com.certimate.manager.repository;

import com.certimate.manager.domain.entity.UserCertification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCertificationRepository extends JpaRepository<UserCertification, Long> {
    int countByUser_Id(Long userId);
    void deleteByUser_Id(Long userId);
}
