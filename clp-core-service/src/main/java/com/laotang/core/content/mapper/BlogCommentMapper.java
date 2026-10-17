package com.laotang.core.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.content.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论数据访问。
 */
@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {
}
