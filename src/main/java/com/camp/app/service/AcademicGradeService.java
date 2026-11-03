package com.camp.app.service;

import com.camp.app.model.OptionItem;

import java.util.List;

public interface AcademicGradeService {
    List<OptionItem> list(Long userId);

    void replace(Long userId, List<Long> gradeIds);
}
