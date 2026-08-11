package com.certimate.manager.repository;

import com.certimate.manager.domain.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
