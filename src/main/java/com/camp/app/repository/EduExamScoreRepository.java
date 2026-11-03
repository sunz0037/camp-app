package com.camp.app.repository;

import com.camp.app.entity.EduExamScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EduExamScoreRepository extends JpaRepository<EduExamScore, Long> {
    long countByExamId(Long examId);

    Optional<EduExamScore> findByExamIdAndStudentId(Long examId, Long studentId);
}
