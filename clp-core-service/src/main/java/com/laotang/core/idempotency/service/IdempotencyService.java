package com.laotang.core.idempotency.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import com.laotang.core.idempotency.mapper.IdempotencyMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 幂等控制。
 *
 * <p>三层防线的第二层:数据库幂等表。第一层是客户端 Idempotency-Key,
 * 第三层是业务表唯一索引(如 order_no)。</p>
 */
@Service
public class IdempotencyService {

    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private static final int EXPIRE_HOURS = 24;

    private final IdempotencyMapper idempotencyMapper;
    private final TransactionTemplate requiresNew;

    public IdempotencyService(IdempotencyMapper idempotencyMapper,
                              PlatformTransactionManager transactionManager) {
        this.idempotencyMapper = idempotencyMapper;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 抢占幂等键。
     *
     * @return 抢到返回新记录,已被占用返回 null
     */
    public IdempotencyRecord tryAcquire(Long userId, String idemKey, String bizType) {
        try {
            return requiresNew.execute(status -> {
                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdemKey(idemKey);
                record.setUserId(userId);
                record.setBizType(bizType);
                record.setStatus(PROCESSING);
                record.setExpireAt(LocalDateTime.now().plusHours(EXPIRE_HOURS));
                idempotencyMapper.insert(record);
                return record;
            });
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    public IdempotencyRecord find(Long userId, String idemKey) {
        IdempotencyRecord record = idempotencyMapper.selectOne(
                new LambdaQueryWrapper<IdempotencyRecord>()
                        .eq(IdempotencyRecord::getUserId, userId)
                        .eq(IdempotencyRecord::getIdemKey, idemKey));

        if (record != null && record.getExpireAt() != null
                && record.getExpireAt().isBefore(LocalDateTime.now())) {
            release(record.getId());
            return null;
        }
        return record;
    }

    public void markSuccess(Long id, Long bizId) {
        requiresNew.executeWithoutResult(status ->
                idempotencyMapper.update(null, new LambdaUpdateWrapper<IdempotencyRecord>()
                        .eq(IdempotencyRecord::getId, id)
                        .set(IdempotencyRecord::getStatus, SUCCESS)
                        .set(IdempotencyRecord::getBizId, String.valueOf(bizId))));
    }

    public void release(Long id) {
        requiresNew.executeWithoutResult(status -> idempotencyMapper.deleteById(id));
    }
}
