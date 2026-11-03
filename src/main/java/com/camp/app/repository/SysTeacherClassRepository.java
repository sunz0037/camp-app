package com.camp.app.repository;

import com.camp.app.entity.SysTeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SysTeacherClassRepository extends JpaRepository<SysTeacherClass, Long> {
    List<SysTeacherClass> findByTeacherId(Long teacherId);

    List<SysTeacherClass> findByClassIdIn(Collection<Long> classIds);

    void deleteByTeacherId(Long teacherId);
}
