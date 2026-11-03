package com.camp.app.api;

public final class Paging {
    private Paging() {
    }

    public static long page(long page) {
        return page < 1 ? 1 : page;
    }

    public static long size(Long pageSize, long size) {
        long value = pageSize != null ? pageSize : size;
        if (value < 1) {
            value = 20;
        }
        if (value > 100) {
            value = 100;
        }
        return value;
    }
}
