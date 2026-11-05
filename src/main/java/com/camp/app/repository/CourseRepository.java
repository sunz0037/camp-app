package com.camp.app.repository;

import com.camp.app.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByCourseName(String courseName);

    @Query("SELECT c FROM Course c WHERE " +
           "(:semester IS NULL OR c.semester = :semester) AND " +
           "(:academicYear IS NULL OR c.academicYear = :academicYear) AND " +
           "(:teacherId IS NULL OR c.teacherId = :teacherId) AND " +
           "(:keyword IS NULL OR c.courseName LIKE %:keyword% OR c.courseCode LIKE %:keyword%)")
    Page<Course> findByFilters(@Param("semester") String semester,
                               @Param("academicYear") String academicYear,
                               @Param("teacherId") Long teacherId,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}

