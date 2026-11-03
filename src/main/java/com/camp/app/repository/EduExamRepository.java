package com.camp.app.repository;

import com.camp.app.entity.EduExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EduExamRepository extends JpaRepository<EduExam, Long> {
    Optional<EduExam> findByExamCode(String examCode);

    long countByExamCode(String examCode);
}
