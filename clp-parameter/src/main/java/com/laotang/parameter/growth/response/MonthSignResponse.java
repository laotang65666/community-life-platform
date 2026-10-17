package com.laotang.parameter.growth.response;

import java.util.List;

/**
 * 本月签到记录。
 */
public record MonthSignResponse(
        int year,
        int month,
        List<Integer> signedDays,
        boolean todaySigned
) {
}
