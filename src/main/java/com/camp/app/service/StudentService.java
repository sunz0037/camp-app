package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ImportResultVo;
import com.camp.app.model.StudentVo;

public interface StudentService {
    PageResult<StudentVo> list(long page, long size, String grade, String className, String major, String keyword);
    StudentVo get(Long id);
    void create(StudentVo student);
    void update(Long id, StudentVo student);
    void delete(Long id);

    String importTemplate();

    ImportResultVo batchImport(String csv);

    String exportCsv();
}

