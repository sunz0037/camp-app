package com.camp.app.repository;

import com.camp.app.entity.ExamScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamScoreRepository extends JpaRepository<ExamScore, Long> {
    @Query("SELECT e FROM ExamScore e WHERE " +
           "(:studentId IS NULL OR e.studentId = :studentId) AND " +
           "(:courseId IS NULL OR e.courseId = :courseId) AND " +
           "(:examScheduleId IS NULL OR e.examScheduleId = :examScheduleId) AND " +
           "(:semester IS NULL OR e.semester = :semester) AND " +
           "(:academicYear IS NULL OR e.academicYear = :academicYear) AND " +
           "(:examType IS NULL OR e.examType = :examType)")
    Page<ExamScore> findByFilters(@Param("studentId") Long studentId,
                                  @Param("courseId") Long courseId,
                                  @Param("examScheduleId") Long examScheduleId,
                                  @Param("semester") String semester,
                                  @Param("academicYear") String academicYear,
                                  @Param("examType") String examType,
                                  Pageable pageable);
    
    /**
     * 根据考试安排ID查询成绩
     */
    Page<ExamScore> findByExamScheduleId(Long examScheduleId, Pageable pageable);
}

