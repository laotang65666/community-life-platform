package com.laotang.core.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.outbox.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表数据访问。
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {
}
