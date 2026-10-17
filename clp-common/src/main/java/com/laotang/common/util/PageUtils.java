package com.laotang.common.util;

/**
 * 分页参数保护。
 */
public final class PageUtils {

    public static final long MAX_SIZE = 100;

    private PageUtils() {
    }

    public static long normalizePage(long page) {
        return Math.max(page, 1);
    }

    public static long normalizeSize(long size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
