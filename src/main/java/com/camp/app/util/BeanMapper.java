package com.camp.app.util;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class BeanMapper {
    public static <T> T map(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map object", e);
        }
    }

    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtils.copyProperties(source, target);
    }

    public static <T> List<T> mapList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null) {
            return null;
        }
        return sourceList.stream()
                .map(source -> map(source, targetClass))
                .collect(Collectors.toList());
    }
}

