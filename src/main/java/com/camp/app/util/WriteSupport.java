package com.camp.app.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Set;

/**
 * 更新时只覆盖非空字段，避免前端局部提交把库内值写成 null。
 */
public final class WriteSupport {
    private WriteSupport() {
    }

    public static void copyNonNull(Object source, Object target, String... ignored) {
        if (source == null || target == null) {
            return;
        }
        Set<String> skip = Set.of(ignored);
        BeanWrapper src = new BeanWrapperImpl(source);
        BeanWrapper dst = new BeanWrapperImpl(target);
        for (PropertyDescriptor descriptor : src.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if ("class".equals(name) || skip.contains(name) || !dst.isWritableProperty(name)) {
                continue;
            }
            Object value = src.getPropertyValue(name);
            if (value != null) {
                dst.setPropertyValue(name, value);
            }
        }
    }
}
