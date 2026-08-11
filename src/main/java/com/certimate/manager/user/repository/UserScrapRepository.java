package com.certimate.manager.user.repository;

import com.certimate.manager.user.entity.UserScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserScrapRepository extends JpaRepository<UserScrap, Long> {
    int countByUser_Id(Long userId);
    List<UserScrap> findByUser_Id(Long userId);
    void deleteByUser_Id(Long userId);
}
