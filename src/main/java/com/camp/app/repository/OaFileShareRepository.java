package com.camp.app.repository;

import com.camp.app.entity.OaFileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OaFileShareRepository extends JpaRepository<OaFileShare, Long> {
    List<OaFileShare> findByFileId(Long fileId);

    List<OaFileShare> findByShareToUserId(Long userId);

    List<OaFileShare> findByShareToRoleId(Long roleId);

    boolean existsByFileId(Long fileId);
}
