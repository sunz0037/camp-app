package com.camp.app.repository;

import com.camp.app.entity.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 班级Repository
 */
@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    
    /**
     * 根据班级代码查询
     */
    Optional<Class> findByClassCode(String classCode);
    
    /**
     * 根据年级ID查询
     */
    List<Class> findByGradeId(Long gradeId);

    List<Class> findByGradeIdIn(java.util.Collection<Long> gradeIds);
    
    /**
     * 根据年级ID和班级序号查询
     */
    Optional<Class> findByGradeIdAndClassNumber(Long gradeId, Integer classNumber);
    
    /**
     * 根据状态查询
     */
    List<Class> findByStatus(Integer status);
    
    /**
     * 根据班主任ID查询
     */
    List<Class> findByHeadTeacherId(Long headTeacherId);
    
    /**
     * 分页查询（支持多条件）
     */
    @Query("SELECT c FROM Class c WHERE " +
           "(:gradeId IS NULL OR c.grade.id = :gradeId) AND " +
           "(:gradeType IS NULL OR c.grade.gradeType = :gradeType) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR c.className LIKE %:keyword% OR c.classCode LIKE %:keyword%) " +
           "ORDER BY c.grade.gradeType, c.grade.gradeLevel, c.classNumber")
    Page<Class> findByConditions(
        @Param("gradeId") Long gradeId,
        @Param("gradeType") String gradeType,
        @Param("status") Integer status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}

