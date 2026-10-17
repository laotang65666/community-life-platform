package com.laotang.core.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 消息 Mapper。
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
