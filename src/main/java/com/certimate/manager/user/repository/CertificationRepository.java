package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
