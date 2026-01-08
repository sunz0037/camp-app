package com.camp.app.repository;

import com.camp.app.entity.PasswordResetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLog, Long> {
    /**
     * 统计指定学号/工号在指定时间内的失败次数
     */
    @Query("SELECT COUNT(l) FROM PasswordResetLog l " +
           "WHERE l.identifier = :identifier " +
           "AND l.status = 0 " +
           "AND l.createdAt >= :since")
    long countFailedAttempts(@Param("identifier") String identifier, 
                            @Param("since") LocalDateTime since);
}

