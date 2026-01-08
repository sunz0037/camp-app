package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Course;
import com.camp.app.model.CourseVo;
import com.camp.app.repository.CourseRepository;
import com.camp.app.service.CourseService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;

    @Override
    public PageResult<CourseVo> list(long page, long size, String semester, String academicYear, Long teacherId, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Course> coursePage = courseRepository.findByFilters(semester, academicYear, teacherId, keyword, pageable);
        List<CourseVo> voList = BeanMapper.mapList(coursePage.getContent(), CourseVo.class);
        
        return PageResult.<CourseVo>builder()
                .page(page)
                .size(size)
                .total(coursePage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public CourseVo detail(Long id) {
        return courseRepository.findById(id)
                .map(course -> BeanMapper.map(course, CourseVo.class))
                .orElse(null);
    }

    @Override
    @Transactional
    public void create(CourseVo vo) {
        Course course = BeanMapper.map(vo, Course.class);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void update(Long id, CourseVo vo) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found: " + id));
        BeanMapper.copyProperties(vo, course);
        course.setId(id);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        courseRepository.deleteById(id);
    }
    
    @Override
    public List<CourseVo> getAllEnabledCourses() {
        return courseRepository.findAll().stream()
            .filter(course -> course.getStatus() != null && course.getStatus() == 1)
            .map(course -> BeanMapper.map(course, CourseVo.class))
            .collect(java.util.stream.Collectors.toList());
    }
}

