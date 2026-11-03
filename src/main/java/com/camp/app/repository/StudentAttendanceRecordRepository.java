package com.camp.app.repository;

import com.camp.app.entity.StudentAttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAttendanceRecordRepository extends JpaRepository<StudentAttendanceRecord, Long> {
    List<StudentAttendanceRecord> findBySummaryId(Long summaryId);

    Optional<StudentAttendanceRecord> findByStudentIdAndAttendanceDateAndCourseId(Long studentId, LocalDate attendanceDate, Long courseId);

    @Query("SELECT r FROM StudentAttendanceRecord r WHERE r.studentId = :studentId AND r.attendanceDate = :attendanceDate AND r.courseId IS NULL")
    Optional<StudentAttendanceRecord> findByStudentIdAndAttendanceDateAndCourseIdIsNull(@Param("studentId") Long studentId,
                                                                                        @Param("attendanceDate") LocalDate attendanceDate);

    @Modifying
    @Query("delete from StudentAttendanceRecord r where r.attendanceDate = :attendanceDate and r.studentId in (select s.id from Student s where s.classId = :classId)")
    void deleteByClassIdAndAttendanceDate(@Param("classId") Long classId, @Param("attendanceDate") LocalDate attendanceDate);

    @Modifying
    @Query("delete from StudentAttendanceRecord r where r.summaryId = :summaryId")
    void deleteBySummaryId(@Param("summaryId") Long summaryId);

    @Query("SELECT r FROM StudentAttendanceRecord r " +
           "JOIN Student s ON r.studentId = s.id WHERE " +
           "(:grade IS NULL OR s.grade = :grade) AND " +
           "(:className IS NULL OR s.className = :className) AND " +
           "(:attendanceDate IS NULL OR r.attendanceDate = :attendanceDate) AND " +
           "(:exceptionType IS NULL OR r.exceptionType = :exceptionType)")
    Page<StudentAttendanceRecord> findByFilters(@Param("grade") String grade,
                                                  @Param("className") String className,
                                                  @Param("attendanceDate") LocalDate attendanceDate,
                                                  @Param("exceptionType") Integer exceptionType,
                                                  Pageable pageable);
}

