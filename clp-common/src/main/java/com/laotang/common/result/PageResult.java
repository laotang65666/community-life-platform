package com.laotang.common.result;

import java.util.List;

/**
 * 统一分页结构。
 */
public record PageResult<T>(List<T> list, long page, long size, long total, long pages) {

    public static <T> PageResult<T> of(List<T> list, long page, long size, long total) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(List.copyOf(list), page, size, total, pages);
    }
}
