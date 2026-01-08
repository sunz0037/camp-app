package com.camp.app.repository;

import com.camp.app.entity.ExamSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试安排Repository
 */
@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    
    /**
     * 分页查询考试安排
     */
    Page<ExamSchedule> findAll(Pageable pageable);
    
    /**
     * 根据创建人查询
     */
    Page<ExamSchedule> findByCreatedBy(Long createdBy, Pageable pageable);
    
    /**
     * 根据年级查询
     */
    Page<ExamSchedule> findByTargetGrade(String targetGrade, Pageable pageable);
    
    /**
     * 根据考试类型查询
     */
    Page<ExamSchedule> findByExamType(String examType, Pageable pageable);
    
    /**
     * 根据状态查询
     */
    Page<ExamSchedule> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 根据是否公开查询
     */
    Page<ExamSchedule> findByIsPublic(Boolean isPublic, Pageable pageable);
    
    /**
     * 根据考试时间范围查询
     */
    @Query("SELECT e FROM ExamSchedule e WHERE e.examDate BETWEEN :startDate AND :endDate")
    Page<ExamSchedule> findByExamDateBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate, 
                                              Pageable pageable);
    
    /**
     * 根据年级和考试类型查询
     */
    Page<ExamSchedule> findByTargetGradeAndExamType(String targetGrade, String examType, Pageable pageable);
    
    /**
     * 查询指定年级的所有正常状态的考试安排
     */
    List<ExamSchedule> findByTargetGradeAndStatus(String targetGrade, Integer status);
    
    /**
     * 查询指定时间范围内的考试安排
     */
    @Query("SELECT e FROM ExamSchedule e WHERE e.examDate >= :startDate AND e.status = 1 ORDER BY e.examDate ASC")
    List<ExamSchedule> findUpcomingExams(@Param("startDate") LocalDateTime startDate);
}

