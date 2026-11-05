package com.camp.app.repository;

import com.camp.app.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    Optional<ExamSchedule> findByClassIdAndCourseIdAndExamDateAndTitle(
            Long classId, Long courseId, LocalDate examDate, String title);
}
