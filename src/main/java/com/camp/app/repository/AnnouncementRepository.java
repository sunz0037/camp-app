package com.camp.app.repository;

import com.camp.app.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @Query("SELECT a FROM Announcement a WHERE " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:isTop IS NULL OR a.isTop = :isTop) AND " +
           "(:isUrgent IS NULL OR a.isUrgent = :isUrgent) AND " +
           "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.content LIKE %:keyword%)")
    Page<Announcement> findByFilters(@Param("category") String category,
                                      @Param("status") Integer status,
                                      @Param("isTop") Integer isTop,
                                      @Param("isUrgent") Integer isUrgent,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Modifying
    @Query("UPDATE Announcement a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
}

