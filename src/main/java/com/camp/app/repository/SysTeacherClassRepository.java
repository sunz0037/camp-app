package com.camp.app.repository;

import com.camp.app.entity.SysTeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysTeacherClassRepository extends JpaRepository<SysTeacherClass, Long> {
    List<SysTeacherClass> findByTeacherId(Long teacherId);

    void deleteByTeacherId(Long teacherId);
}
