package com.camp.app.repository;

import com.camp.app.entity.ClassAttendanceDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ClassAttendanceDailyRepository extends JpaRepository<ClassAttendanceDaily, Long> {
    Optional<ClassAttendanceDaily> findByAttendanceDateAndGradeAndClassName(
            LocalDate attendanceDate, String grade, String className);

    @Query("SELECT c FROM ClassAttendanceDaily c WHERE " +
           "(:grade IS NULL OR c.grade = :grade) AND " +
           "(:className IS NULL OR c.className = :className) AND " +
           "(:attendanceDate IS NULL OR c.attendanceDate = :attendanceDate)")
    Page<ClassAttendanceDaily> findByFilters(@Param("grade") String grade,
                                              @Param("className") String className,
                                              @Param("attendanceDate") LocalDate attendanceDate,
                                              Pageable pageable);
}

