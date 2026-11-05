package com.camp.app.repository;

import com.camp.app.entity.ExamScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamScoreRepository extends JpaRepository<ExamScore, Long> {
    Optional<ExamScore> findByExamIdAndStudentId(Long examId, Long studentId);

    long countByExamId(Long examId);
}
