package com.camp.app.repository;

import com.camp.app.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级Repository
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    
    /**
     * 根据年级代码查询
     */
    Optional<Grade> findByGradeCode(String gradeCode);
    
    /**
     * 根据年级类型查询
     */
    List<Grade> findByGradeType(String gradeType);
    
    /**
     * 根据年级类型和级别查询
     */
    Optional<Grade> findByGradeTypeAndGradeLevel(String gradeType, Integer gradeLevel);
    
    /**
     * 根据状态查询
     */
    List<Grade> findByStatus(Integer status);
    
    /**
     * 分页查询（支持多条件）
     */
    @Query("SELECT g FROM Grade g WHERE " +
           "(:gradeType IS NULL OR g.gradeType = :gradeType) AND " +
           "(:status IS NULL OR g.status = :status) AND " +
           "(:keyword IS NULL OR g.gradeName LIKE %:keyword%) " +
           "ORDER BY g.gradeType, g.gradeLevel")
    Page<Grade> findByConditions(
        @Param("gradeType") String gradeType,
        @Param("status") Integer status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}

