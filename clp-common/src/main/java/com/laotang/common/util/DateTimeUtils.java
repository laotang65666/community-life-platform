package com.laotang.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 对外统一使用 ISO 8601 + 东八区,例如 2026-10-17T15:14:31+08:00。
 */
public final class DateTimeUtils {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DateTimeUtils() {
    }

    public static String toIso(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).format(FORMATTER);
    }
}
