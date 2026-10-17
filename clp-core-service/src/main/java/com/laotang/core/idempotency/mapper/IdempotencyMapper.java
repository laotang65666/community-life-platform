package com.laotang.core.idempotency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.idempotency.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录数据访问。
 */
@Mapper
public interface IdempotencyMapper extends BaseMapper<IdempotencyRecord> {
}
