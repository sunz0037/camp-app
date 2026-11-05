package com.camp.app.repository;

import com.camp.app.entity.OaVisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OaVisitorLogRepository extends JpaRepository<OaVisitorLog, Long> {
    List<OaVisitorLog> findByVisitorIdOrderByIdAsc(Long visitorId);

    void deleteByVisitorId(Long visitorId);
}
