package com.laotang.core.growth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.growth.entity.PointLog;
import com.laotang.core.growth.entity.SignRecord;
import com.laotang.core.growth.mapper.PointLogMapper;
import com.laotang.core.growth.mapper.SignRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 签到的事务边界:写签到审计 + 发积分 + 记流水。
 */
@Service
@RequiredArgsConstructor
public class GrowthTxService {

    private static final int DAILY_POINTS = 5;
    private static final int STREAK_BONUS = 20;
    private static final int STREAK_DAYS = 7;

    private final SignRecordMapper signMapper;
    private final PointLogMapper pointLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public SignInResult doSignIn(Long userId, LocalDate today) {
        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        record.setYear(today.getYear());
        record.setMonth(today.getMonthValue());
        record.setCreatedAt(LocalDateTime.now());

        try {
            signMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.ALREADY_SIGNED);
        }

        int streak = countStreak(userId, today);
        boolean bonus = streak % STREAK_DAYS == 0;
        int points = bonus ? DAILY_POINTS + STREAK_BONUS : DAILY_POINTS;

        int balance = currentBalance(userId);
        int dailyBalance = balance + DAILY_POINTS;
        insertPointLog(userId, DAILY_POINTS, dailyBalance, "SIGN_IN", today.toString(), "每日签到");

        int finalBalance = dailyBalance;
        if (bonus) {
            finalBalance += STREAK_BONUS;
            insertPointLog(userId, STREAK_BONUS, finalBalance,
                    "SIGN_IN_STREAK", today.toString(), "连续签到 " + STREAK_DAYS + " 天奖励");
        }

        return new SignInResult(streak, points, finalBalance);
    }

    private int countStreak(Long userId, LocalDate today) {
        List<SignRecord> recent = signMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .ge(SignRecord::getSignDate, today.minusDays(STREAK_DAYS - 1))
                .orderByDesc(SignRecord::getSignDate));

        Set<LocalDate> dates = recent.stream()
                .map(SignRecord::getSignDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = today;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int currentBalance(Long userId) {
        PointLog latest = pointLogMapper.selectOne(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId)
                .orderByDesc(PointLog::getId)
                .last("LIMIT 1"));
        return latest == null || latest.getBalance() == null ? 0 : latest.getBalance();
    }

    private void insertPointLog(Long userId, int change, int balance,
                                String source, String bizId, String remark) {
        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setChangeAmount(change);
        log.setBalance(balance);
        log.setSource(source);
        log.setBizId(bizId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());

        try {
            pointLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            // uk_user_source_biz:同一来源同一业务只发一次
        }
    }

    public record SignInResult(int continuousDays, int pointsAwarded, int balance) {
    }
}
