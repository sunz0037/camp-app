package com.camp.app.repository;

import com.camp.app.entity.VisitorRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface VisitorRegistrationRepository extends JpaRepository<VisitorRegistration, Long> {
    @Query("SELECT v FROM VisitorRegistration v WHERE " +
           "(:visitDate IS NULL OR v.visitDate = :visitDate) AND " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:visitTargetUserId IS NULL OR v.visitTargetUserId = :visitTargetUserId) AND " +
           "(:keyword IS NULL OR v.visitorName LIKE %:keyword% OR v.visitorPhone LIKE %:keyword%)")
    Page<VisitorRegistration> findByFilters(@Param("visitDate") LocalDate visitDate,
                                            @Param("status") Integer status,
                                            @Param("visitTargetUserId") Long visitTargetUserId,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);
}

