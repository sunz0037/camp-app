package com.camp.app.repository;

import com.camp.app.entity.SysAffairsGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysAffairsGradeRepository extends JpaRepository<SysAffairsGrade, Long> {
    List<SysAffairsGrade> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
