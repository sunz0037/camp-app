package com.camp.app.repository;

import com.camp.app.entity.ClassCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 班级授课科目关联Repository
 */
@Repository
public interface ClassCourseRepository extends JpaRepository<ClassCourse, Long> {
    
    /**
     * 根据班级ID查询所有关联的科目
     */
    List<ClassCourse> findByClassEntityId(Long classId);
    
    /**
     * 根据班级ID和状态查询关联的科目
     */
    List<ClassCourse> findByClassEntityIdAndStatus(Long classId, Integer status);
    
    /**
     * 根据科目ID查询所有关联的班级
     */
    List<ClassCourse> findByCourseId(Long courseId);

    List<ClassCourse> findByTeacher_Id(Long teacherId);
    
    /**
     * 根据班级ID和科目ID查询关联（用于检查是否已存在）
     */
    Optional<ClassCourse> findByClassEntityIdAndCourseId(Long classId, Long courseId);
    
    /**
     * 根据班级ID、科目ID、学期、学年查询（用于唯一性检查）
     */
    @Query("SELECT cc FROM ClassCourse cc WHERE cc.classEntity.id = :classId " +
           "AND cc.course.id = :courseId " +
           "AND (:semester IS NULL OR cc.semester = :semester) " +
           "AND (:academicYear IS NULL OR cc.academicYear = :academicYear)")
    Optional<ClassCourse> findByClassAndCourseAndSemesterAndYear(
        @Param("classId") Long classId,
        @Param("courseId") Long courseId,
        @Param("semester") String semester,
        @Param("academicYear") String academicYear
    );
    
    /**
     * 删除班级的所有科目关联
     */
    void deleteByClassEntityId(Long classId);
    
    /**
     * 删除科目的所有班级关联
     */
    void deleteByCourseId(Long courseId);
}

