package com.camp.app.repository;

import com.camp.app.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    List<AnnouncementRead> findByUserIdAndAnnouncementIdIn(Long userId, Collection<Long> announcementIds);

    long countByUserIdAndAnnouncementIdIn(Long userId, Collection<Long> announcementIds);
}
