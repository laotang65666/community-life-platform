package com.laotang.core.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话 Mapper。
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
