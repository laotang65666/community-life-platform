package com.laotang.core.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.common.util.DateTimeUtils;
import com.laotang.core.growth.entity.PointLog;
import com.laotang.core.growth.entity.SignRecord;
import com.laotang.core.growth.mapper.PointLogMapper;
import com.laotang.core.growth.mapper.SignRecordMapper;
import com.laotang.core.growth.service.GrowthTxService;
import com.laotang.core.growth.service.IGrowthService;
import com.laotang.parameter.growth.response.MonthSignResponse;
import com.laotang.parameter.growth.response.PointLogResponse;
import com.laotang.parameter.growth.response.PointsResponse;
import com.laotang.parameter.growth.response.SignInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户成长服务实现。
 *
 * <p>签到判断用 Redis Bitmap:一个用户一个月一个 key,offset = 当月第几天 - 1。
 * DB 的 tb_sign 负责审计和连续签到统计,积分流水负责余额和幂等。</p>
 */
@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements IGrowthService {

    private static final Duration SIGN_KEY_TTL = Duration.ofDays(100);

    private final StringRedisTemplate redis;
    private final GrowthTxService growthTxService;
    private final SignRecordMapper signMapper;
    private final PointLogMapper pointLogMapper;

    @Override
    public SignInResponse signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String key = RedisKey.sign(userId, today);
        long offset = today.getDayOfMonth() - 1L;

        Boolean signed = redis.opsForValue().getBit(key, offset);
        if (Boolean.TRUE.equals(signed)) {
            throw new BusinessException(ErrorCode.ALREADY_SIGNED);
        }

        // 先在数据库完成签到 + 积分事务,成功后再写 Bitmap
        GrowthTxService.SignInResult result = growthTxService.doSignIn(userId, today);

        redis.opsForValue().setBit(key, offset, true);
        redis.expire(key, SIGN_KEY_TTL);

        return new SignInResponse(
                true, result.continuousDays(), result.pointsAwarded(), result.balance());
    }

    @Override
    public MonthSignResponse currentMonth(Long userId) {
        LocalDate today = LocalDate.now();

        List<SignRecord> records = signMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getYear, today.getYear())
                .eq(SignRecord::getMonth, today.getMonthValue())
                .orderByAsc(SignRecord::getSignDate));

        List<Integer> days = records.stream()
                .map(record -> record.getSignDate().getDayOfMonth())
                .toList();

        boolean todaySigned = records.stream()
                .anyMatch(record -> record.getSignDate().equals(today));

        return new MonthSignResponse(
                today.getYear(), today.getMonthValue(), days, todaySigned);
    }

    @Override
    public PointsResponse points(Long userId) {
        PointLog latest = pointLogMapper.selectOne(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId)
                .orderByDesc(PointLog::getId)
                .last("LIMIT 1"));

        int balance = latest == null || latest.getBalance() == null ? 0 : latest.getBalance();
        return new PointsResponse(balance);
    }

    @Override
    public PageResult<PointLogResponse> pointLogs(Long userId, long page, long size) {
        Page<PointLog> result = pointLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getId));

        List<PointLogResponse> list = result.getRecords().stream()
                .map(log -> new PointLogResponse(
                        String.valueOf(log.getId()),
                        log.getChangeAmount(),
                        log.getBalance(),
                        log.getSource(),
                        log.getBizId(),
                        log.getRemark(),
                        DateTimeUtils.toIso(log.getCreatedAt())))
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
