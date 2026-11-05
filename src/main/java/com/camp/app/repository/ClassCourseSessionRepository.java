package com.camp.app.repository;

import com.camp.app.entity.ClassCourseSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ClassCourseSessionRepository extends JpaRepository<ClassCourseSession, Long> {
    List<ClassCourseSession> findByClassCourseId(Long classCourseId);

    List<ClassCourseSession> findByClassCourseIdIn(Collection<Long> classCourseIds);

    void deleteByClassCourseId(Long classCourseId);
}
