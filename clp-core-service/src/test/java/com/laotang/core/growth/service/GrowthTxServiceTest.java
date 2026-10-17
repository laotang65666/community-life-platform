package com.laotang.core.growth.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.growth.entity.PointLog;
import com.laotang.core.growth.entity.SignRecord;
import com.laotang.core.growth.mapper.PointLogMapper;
import com.laotang.core.growth.mapper.SignRecordMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthTxServiceTest {

    @Mock
    private SignRecordMapper signMapper;
    @Mock
    private PointLogMapper pointLogMapper;
    @InjectMocks
    private GrowthTxService growthTxService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SignRecord.class);
        TableInfoHelper.initTableInfo(assistant, PointLog.class);
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
    @DisplayName("首次签到:连续 1 天,加 5 分")
    void doSignIn_firstDay() {
        LocalDate today = LocalDate.now();
        when(signMapper.insert(any(SignRecord.class))).thenReturn(1);
        when(signMapper.selectList(any())).thenReturn(List.of(sign(today)));
        when(pointLogMapper.selectOne(any())).thenReturn(null);

        GrowthTxService.SignInResult result = growthTxService.doSignIn(1L, today);

        assertThat(result.continuousDays()).isEqualTo(1);
        assertThat(result.pointsAwarded()).isEqualTo(5);
        assertThat(result.balance()).isEqualTo(5);
        verify(pointLogMapper, times(1)).insert(any(PointLog.class));
    }

    @Test
    @DisplayName("连续第 7 天:额外加 20 分")
    void doSignIn_seventhDay() {
        LocalDate today = LocalDate.now();
        when(signMapper.insert(any(SignRecord.class))).thenReturn(1);
        when(signMapper.selectList(any())).thenReturn(List.of(
                sign(today),
                sign(today.minusDays(1)),
                sign(today.minusDays(2)),
                sign(today.minusDays(3)),
                sign(today.minusDays(4)),
                sign(today.minusDays(5)),
                sign(today.minusDays(6))));

        PointLog latest = new PointLog();
        latest.setBalance(30);
        when(pointLogMapper.selectOne(any())).thenReturn(latest);

        GrowthTxService.SignInResult result = growthTxService.doSignIn(1L, today);

        assertThat(result.continuousDays()).isEqualTo(7);
        assertThat(result.pointsAwarded()).isEqualTo(25);
        assertThat(result.balance()).isEqualTo(55);
        verify(pointLogMapper, times(2)).insert(any(PointLog.class));
    }

    @Test
    @DisplayName("重复签到 -> 9001")
    void doSignIn_duplicate() {
        when(signMapper.insert(any(SignRecord.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        BusinessException ex = catchThrowableOfType(
                () -> growthTxService.doSignIn(1L, LocalDate.now()), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_SIGNED);
    }
}
