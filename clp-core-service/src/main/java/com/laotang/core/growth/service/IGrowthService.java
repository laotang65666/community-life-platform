package com.laotang.core.growth.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.growth.response.MonthSignResponse;
import com.laotang.parameter.growth.response.PointLogResponse;
import com.laotang.parameter.growth.response.PointsResponse;
import com.laotang.parameter.growth.response.SignInResponse;

/**
 * 用户成长服务:签到与积分。
 */
public interface IGrowthService {

    SignInResponse signIn(Long userId);

    MonthSignResponse currentMonth(Long userId);

    PointsResponse points(Long userId);

    PageResult<PointLogResponse> pointLogs(Long userId, long page, long size);
}
