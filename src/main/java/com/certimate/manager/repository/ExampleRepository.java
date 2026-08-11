package com.certimate.manager.repository;

import com.certimate.manager.domain.entity.Example;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<Example, Long> {
}
