package com.camp.app.repository;

import com.camp.app.entity.AnnouncementScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AnnouncementScopeRepository extends JpaRepository<AnnouncementScope, Long> {
    List<AnnouncementScope> findByAnnouncementId(Long announcementId);

    List<AnnouncementScope> findByAnnouncementIdIn(Collection<Long> announcementIds);

    void deleteByAnnouncementId(Long announcementId);
}
