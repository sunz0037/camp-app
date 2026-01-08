package com.camp.app.repository;

import com.camp.app.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByStudentNo(String studentNo);
    
    /**
     * 根据用户ID查找学生
     */
    Optional<Student> findByUserId(Long userId);
    
    /**
     * 根据班级ID查找学生列表
     */
    List<Student> findByClassId(Long classId);
    
    /**
     * 根据年级ID查找学生列表
     */
    List<Student> findByGradeId(Long gradeId);
    
    /**
     * 统计班级学生数（仅统计在读学生）
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.classId = :classId AND s.status = 1")
    Long countByClassId(@Param("classId") Long classId);
}

