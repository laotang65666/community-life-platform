package com.laotang.core.growth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.growth.entity.PointLog;
import com.laotang.core.growth.entity.SignRecord;
import com.laotang.core.growth.mapper.PointLogMapper;
import com.laotang.core.growth.mapper.SignRecordMapper;
import com.laotang.core.growth.service.GrowthTxService;
import com.laotang.parameter.growth.response.MonthSignResponse;
import com.laotang.parameter.growth.response.PointsResponse;
import com.laotang.parameter.growth.response.SignInResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private GrowthTxService growthTxService;
    @Mock
    private SignRecordMapper signMapper;
    @Mock
    private PointLogMapper pointLogMapper;
    @InjectMocks
    private GrowthServiceImpl growthService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SignRecord.class);
        TableInfoHelper.initTableInfo(assistant, PointLog.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    private SignRecord sign(LocalDate date) {
        SignRecord record = new SignRecord();
        record.setUserId(1L);
        record.setSignDate(date);
        record.setYear(date.getYear());
        record.setMonth(date.getMonthValue());
        return record;
    }

    @Test
    @DisplayName("今天已签到 -> 9001")
    void signIn_alreadySigned() {
        when(valueOps.getBit(anyString(), anyLong())).thenReturn(true);

        BusinessException ex = catchThrowableOfType(
                () -> growthService.signIn(1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_SIGNED);
    }

    @Test
    @DisplayName("首次签到 -> 返回积分并写 Bitmap")
    void signIn_success() {
        when(valueOps.getBit(anyString(), anyLong())).thenReturn(false);
        when(growthTxService.doSignIn(eq(1L), any(LocalDate.class)))
                .thenReturn(new GrowthTxService.SignInResult(1, 5, 5));

        SignInResponse response = growthService.signIn(1L);

        assertThat(response.signed()).isTrue();
        assertThat(response.continuousDays()).isEqualTo(1);
        assertThat(response.pointsAwarded()).isEqualTo(5);
        assertThat(response.balance()).isEqualTo(5);

        LocalDate today = LocalDate.now();
        String key = RedisKey.sign(1L, today);
        verify(valueOps).setBit(eq(key), eq((long) today.getDayOfMonth() - 1), eq(true));
        verify(redis).expire(eq(key), any(Duration.class));
    }

    @Test
    @DisplayName("本月签到记录")
    void currentMonth() {
        LocalDate today = LocalDate.now();
        when(signMapper.selectList(any())).thenReturn(
                List.of(sign(today), sign(today.minusDays(1))));

        MonthSignResponse response = growthService.currentMonth(1L);

        assertThat(response.year()).isEqualTo(today.getYear());
        assertThat(response.month()).isEqualTo(today.getMonthValue());
        assertThat(response.signedDays()).contains(today.getDayOfMonth());
        assertThat(response.todaySigned()).isTrue();
    }

    @Test
    @DisplayName("积分余额取最新流水")
    void points() {
        PointLog log = new PointLog();
        log.setBalance(50);
        log.setCreatedAt(LocalDateTime.now());
        when(pointLogMapper.selectOne(any())).thenReturn(log);

        PointsResponse response = growthService.points(1L);

        assertThat(response.balance()).isEqualTo(50);
    }

    @Test
    @DisplayName("积分流水分页")
    void pointLogs() {
        PointLog log = new PointLog();
        log.setId(10L);
        log.setUserId(1L);
        log.setChangeAmount(5);
        log.setBalance(5);
        log.setSource("SIGN_IN");
        log.setBizId("2026-10-17");
        log.setRemark("每日签到");
        log.setCreatedAt(LocalDateTime.now());

        Page<PointLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log));
        page.setTotal(1);

        when(pointLogMapper.selectPage(any(), any())).thenReturn(page);

        var result = growthService.pointLogs(1L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().source()).isEqualTo("SIGN_IN");
    }
}
